import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
PROJECT = 'yamlscript'
OTHER_PROJECT = 'yamlstar' if PROJECT == 'yamlscript' else 'yamlscript'
TOKENS = {'yamlstar': 'fixture-star-STAR',
          'yamlscript': 'fixture-script-SCRP'}


class DubCredentialTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.runtime = (os.environ.get('YS_TEST_RUNTIME') or
                        os.environ.get('YS') or shutil.which('ys'))
        self.assertTrue(self.runtime, 'Run through the project Makes shell')
        self.secrets = self.directory / 'secrets.yaml'
        self.secrets.write_text(
            'dlang:\n  token:\n    yamlstar: ' + TOKENS['yamlstar'] +
            '\n    yamlscript: ' + TOKENS['yamlscript'] + '\n'
        )
        util = self.directory / 'util'
        util.mkdir()
        shutil.copyfile(ROOT / 'util/release-d', util / 'release-d')
        # Replace the repository publisher so tests only invoke the update hook.
        stub = 'set -euo pipefail\nmain() { after_release; }\n'
        if PROJECT == 'yamlstar':
            stub += 'main "$@"\n'
        (util / 'release-repo.bash').write_text(stub)
        self.request = self.directory / 'request.json'
        curl = self.directory / 'curl'
        curl.write_text("""#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
args = sys.argv[1:]
payload = json.loads(args[args.index('-d') + 1])
Path(os.environ['DUB_TEST_REQUEST']).write_text(json.dumps(payload))
print(os.environ['DUB_TEST_RESPONSE'])
sys.exit(int(os.environ.get('DUB_TEST_CURL_EXIT', '0')))
""")
        curl.chmod(0o755)
        self.gh_log = self.directory / 'gh.json'
        gh = self.directory / 'gh'
        gh.write_text("""#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
log = Path(os.environ['DUB_TEST_GH_LOG'])
entries = json.loads(log.read_text()) if log.exists() else []
entries.append({'args': sys.argv[1:], 'value': sys.stdin.read(),
                'auth_matches': os.environ.get('GH_TOKEN') ==
                    os.environ.get('DUB_TEST_EXPECTED_GITHUB_TOKEN')})
log.write_text(json.dumps(entries))
""")
        gh.chmod(0o755)
        self.env = dict(os.environ)
        for key in ['DUB_UPDATE_SECRET', 'YS_RELEASE_DRYRUN',
                    'YS_RELEASE_DRY_RUN', 'YS_RELEASE_VERBOSE']:
            self.env.pop(key, None)
        self.env.update(
            YS=self.runtime, YS_SECRETS_FILE=str(self.secrets),
            YAMLSCRIPT_ROOT=str(self.directory),
            DUB_TEST_REQUEST=str(self.request),
            DUB_TEST_GH_LOG=str(self.gh_log),
            DUB_TEST_RESPONSE='"Queued package update"',
            PATH=str(self.directory) + os.pathsep + self.env['PATH'],
        )

    def run_update(self, **extra):
        env = dict(self.env, **extra)
        return subprocess.run(
            ['bash', str(self.directory / 'util/release-d')],
            env=env, capture_output=True, text=True,
        )

    def run_credentials(self, option, input=None):
        return subprocess.run(
            [self.runtime, str(ROOT / 'util' / (PROJECT + '-secrets')),
             option], env=self.env, input=input,
            capture_output=True, text=True,
        )

    def read_token(self, project):
        result = subprocess.run(
            [self.runtime, '-e',
             'say: yaml/load(read(ENV.YS_SECRETS_FILE)).dlang.token.' + project],
            env=self.env, capture_output=True, text=True, check=True,
        )
        return result.stdout.strip()

    def test_local_release_selects_its_project_token(self):
        result = self.run_update()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(self.request.read_text())['secret'],
                         TOKENS[PROJECT])
        self.assertIn('Queued package update', result.stdout)

    def test_environment_override_is_preserved(self):
        result = self.run_update(DUB_UPDATE_SECRET='environment-override')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(self.request.read_text())['secret'],
                         'environment-override')

    def test_missing_project_token_does_not_use_the_other_token(self):
        self.secrets.write_text('dlang:\n  token:\n    ' + OTHER_PROJECT +
                                ': ' + TOKENS[OTHER_PROJECT] + '\n')
        result = self.run_update()
        self.assertEqual(result.returncode, 1)
        self.assertIn('.dlang.token.' + PROJECT, result.stderr)
        self.assertFalse(self.request.exists())

    def test_flat_legacy_token_is_not_used(self):
        self.secrets.write_text('dlang:\n  token: old-shared-token\n')
        result = self.run_update()
        self.assertEqual(result.returncode, 1)
        self.assertFalse(self.request.exists())

    def test_invalid_secret_fails_even_with_http_success(self):
        result = self.run_update(DUB_TEST_RESPONSE='"Invalid secret"')
        self.assertEqual(result.returncode, 1)
        self.assertIn('rejected DUB_UPDATE_SECRET', result.stderr)
        self.assertNotIn(TOKENS[PROJECT], result.stdout + result.stderr)

    def test_http_failure_is_propagated(self):
        result = self.run_update(DUB_TEST_CURL_EXIT='22')
        self.assertEqual(result.returncode, 22)

    def test_dry_run_does_not_contact_the_registry(self):
        result = self.run_update(YS_RELEASE_DRYRUN='1')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(self.request.exists())

    def test_credential_status_masks_the_selected_token(self):
        result = self.run_credentials('--list')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('****' + TOKENS[PROJECT][-4:], result.stdout)
        self.assertNotIn('****' + TOKENS[OTHER_PROJECT][-4:], result.stdout)
        self.assertNotIn(TOKENS[PROJECT], result.stdout)

    def test_secret_publication_selects_its_project_token(self):
        result = self.run_credentials('--publish')
        self.assertEqual(result.returncode, 0, result.stderr)
        entries = json.loads(self.gh_log.read_text())
        dub = [e for e in entries if 'DUB_UPDATE_SECRET' in e['args']]
        self.assertEqual(len(dub), 1)
        self.assertEqual(dub[0]['value'], TOKENS[PROJECT])
        self.assertIn('yaml/' + PROJECT, dub[0]['args'])

    def test_secret_publication_uses_github_auth_from_the_shared_file(self):
        self.secrets.write_text(self.secrets.read_text() +
                                'github:\n  token: fixture-github-token\n')
        self.env['DUB_TEST_EXPECTED_GITHUB_TOKEN'] = 'fixture-github-token'
        result = self.run_credentials('--publish')
        self.assertEqual(result.returncode, 0, result.stderr)
        entries = json.loads(self.gh_log.read_text())
        self.assertTrue(entries)
        self.assertTrue(all(entry['auth_matches'] for entry in entries))

    def test_credential_update_preserves_the_other_project_token(self):
        result = self.run_credentials('--update=d', input='replacement-token\n')
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.read_token(PROJECT), 'replacement-token')
        self.assertEqual(self.read_token(OTHER_PROJECT), TOKENS[OTHER_PROJECT])


if __name__ == '__main__':
    unittest.main()
