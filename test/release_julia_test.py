import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
TOKEN_VARIABLES = ['YS_GH_TOKEN', 'JULIA_GITHUB_TOKEN',
                   'GH_TOKEN', 'GITHUB_TOKEN']


class JuliaRegistrationTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.runtime = (os.environ.get('YS_TEST_RUNTIME') or
                        os.environ.get('YS') or shutil.which('ys'))
        self.assertTrue(self.runtime, 'Run through the project Makes shell')
        self.secrets = self.directory / 'secrets.yaml'
        self.secrets.write_text('github:\n  token: fixture-github-token\n')
        self.request = self.directory / 'request.json'
        curl = self.directory / 'curl'
        curl.write_text('''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
Path(os.environ['JULIA_TEST_REQUEST']).write_text(json.dumps(sys.argv[1:]))
print('{"html_url":"https://fixture.invalid/registration"}')
sys.exit(int(os.environ.get('JULIA_TEST_CURL_EXIT', '0')))
''')
        curl.chmod(0o755)
        self.env = dict(os.environ)
        for key in TOKEN_VARIABLES + ['YS_RELEASE_DRYRUN', 'YS_GH_API_URL']:
            self.env.pop(key, None)
        self.env.update(
            YS=self.runtime, YS_SECRETS_FILE=str(self.secrets),
            YAMLSCRIPT_ROOT=str(ROOT), JULIA_TEST_REQUEST=str(self.request),
            PATH=str(self.directory) + os.pathsep + self.env['PATH'],
        )
        self.commit = subprocess.check_output(
            ['git', '-C', str(ROOT), 'rev-parse', 'HEAD'], text=True,
        ).strip()

    def run_helper(self, **extra):
        return subprocess.run(
            ['bash', str(ROOT / 'util/release-julia')],
            env=dict(self.env, **extra), capture_output=True, text=True,
        )

    def assert_request(self, token):
        args = json.loads(self.request.read_text())
        self.assertIn('Authorization: Bearer ' + token, args)
        self.assertIn('https://api.github.com/repos/yaml/yamlscript/' +
                      'commits/' + self.commit + '/comments', args)
        self.assertIn('--fail-with-body', args)
        self.assertEqual(json.loads(args[args.index('-d') + 1]),
                         {'body': '@JuliaRegistrator register subdir=julia\n'})

    def test_uses_the_shared_file_and_current_commit(self):
        result = self.run_helper()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_request('fixture-github-token')
        self.assertNotIn('fixture-github-token', result.stdout + result.stderr)

    def test_environment_tokens_take_precedence_in_order(self):
        for index, name in enumerate(TOKEN_VARIABLES):
            with self.subTest(variable=name):
                env = {key: 'fixture-' + key for key in TOKEN_VARIABLES[index:]}
                result = self.run_helper(**env)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assert_request('fixture-' + name)
                self.assertNotIn('fixture-' + name,
                                 result.stdout + result.stderr)

    def test_missing_file_and_token_do_not_send_a_request(self):
        self.secrets.unlink()
        result = self.run_helper()
        self.assertEqual(result.returncode, 1)
        self.assertIn('.github.token', result.stderr)
        self.assertFalse(self.request.exists())

    def test_missing_github_entry_does_not_send_nil_as_a_token(self):
        self.secrets.write_text('{}\n')
        result = self.run_helper()
        self.assertEqual(result.returncode, 1)
        self.assertFalse(self.request.exists())

    def test_http_failure_is_propagated(self):
        result = self.run_helper(JULIA_TEST_CURL_EXIT='22')
        self.assertEqual(result.returncode, 22)
        self.assertNotIn('fixture-github-token', result.stdout + result.stderr)

    def test_dry_run_does_not_require_a_token_or_post_a_comment(self):
        self.secrets.unlink()
        result = self.run_helper(YS_RELEASE_DRYRUN='1')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(self.commit, result.stdout)
        self.assertFalse(self.request.exists())

    def test_make_release_uses_the_managed_runtime_and_shared_file(self):
        result = subprocess.run(
            ['make', '--no-print-directory', '-C', str(ROOT / 'julia'),
             'release', 'YS=' + self.runtime],
            env=self.env, capture_output=True, text=True,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assert_request('fixture-github-token')
        self.assertNotIn('fixture-github-token', result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
