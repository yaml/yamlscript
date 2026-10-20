import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
PROJECT = 'yamlscript'
TOKEN = 'fixture-canonical-HEX1'


class HexCredentialTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.runtime = (os.environ.get('YS_TEST_RUNTIME') or
                        os.environ.get('YS') or shutil.which('ys'))
        self.assertTrue(self.runtime, 'Run through the project Makes shell')
        self.secrets = self.directory / 'secrets.yaml'
        self.legacy = (
            'elixir:\n  uses: hex\n  token: fixture-legacy-ELIX\n'
            'erlang:\n  uses: hex\n  token: fixture-legacy-ERLA\n'
        )
        self.secrets.write_text('hex:\n  token: ' + TOKEN + '\n' + self.legacy)
        for project in ['yamlstar', 'yamlscript']:
            (self.directory / ('.' + project + '-secrets.yaml')).symlink_to(
                self.secrets)
        self.gh_log = self.directory / 'gh.json'
        gh = self.directory / 'gh'
        gh.write_text('''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
log = Path(os.environ['HEX_TEST_GH_LOG'])
entries = json.loads(log.read_text()) if log.exists() else []
entries.append({'args': sys.argv[1:], 'value': sys.stdin.read()})
log.write_text(json.dumps(entries))
''')
        gh.chmod(0o755)
        self.env = dict(os.environ)
        self.env.pop('YS_RELEASE_DRYRUN', None)
        self.env.update(
            YS=self.runtime, YS_SECRETS_FILE=str(self.secrets),
            HEX_TEST_GH_LOG=str(self.gh_log), HOME=str(self.directory),
            HEX_API_KEY='',
            PATH=os.pathsep.join([str(self.directory),
                                  str(Path(self.runtime).parent),
                                  self.env['PATH']]),
        )

    def run_credentials(self, option, input=None):
        result = subprocess.run(
            [self.runtime, str(ROOT / 'util' / (PROJECT + '-secrets')),
             option], env=self.env, input=input,
            capture_output=True, text=True,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        return result.stdout

    def published_hex(self):
        entries = (json.loads(self.gh_log.read_text())
                   if self.gh_log.exists() else [])
        return [e for e in entries if 'HEX_API_KEY' in e['args']]

    def read_token(self, entry):
        result = subprocess.run(
            [self.runtime, '-e',
             'say: yaml/load(read(ENV.YS_SECRETS_FILE)).' + entry + '.token'],
            env=self.env, capture_output=True, text=True, check=True,
        )
        return result.stdout.strip()

    def test_list_uses_only_the_canonical_hex_entry(self):
        output = self.run_credentials('--list')
        self.assertIn('****HEX1', output)
        self.assertNotIn('****ELIX', output)
        self.assertNotIn('****ERLA', output)
        self.assertNotIn(TOKEN, output)

    def test_publish_sets_the_canonical_key_once_despite_legacy_entries(self):
        self.run_credentials('--publish')
        entries = self.published_hex()
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0]['value'], TOKEN)
        self.assertIn('yaml/' + PROJECT, entries[0]['args'])

    def test_publish_works_with_only_the_canonical_entry(self):
        self.secrets.write_text('hex:\n  token: ' + TOKEN + '\n')
        self.run_credentials('--publish')
        self.assertEqual([e['value'] for e in self.published_hex()], [TOKEN])

    def test_missing_hex_does_not_publish_a_legacy_key(self):
        self.secrets.write_text(self.legacy)
        self.run_credentials('--publish')
        self.assertEqual(self.published_hex(), [])

    def test_update_changes_hex_without_modifying_legacy_entries(self):
        self.run_credentials('--update=hex', input='replacement-key\n')
        self.assertEqual(self.read_token('hex'), 'replacement-key')
        self.assertEqual(self.read_token('elixir'), 'fixture-legacy-ELIX')
        self.assertEqual(self.read_token('erlang'), 'fixture-legacy-ERLA')

    def test_update_can_create_the_canonical_entry(self):
        self.secrets.write_text(self.legacy)
        self.run_credentials('--update=hex', input='replacement-key\n')
        self.assertEqual(self.read_token('hex'), 'replacement-key')

    def make_key(self, binding, override=''):
        rule = 'hex-test: ; @printf "%s\\n" "$(HEX-API-KEY)"'
        result = subprocess.run(
            ['make', '--no-print-directory', '-C', str(ROOT / binding),
             '--eval=' + rule, 'hex-test'],
            env=dict(self.env, HEX_API_KEY=override),
            capture_output=True, text=True,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        return result.stdout.strip()

    def test_local_releases_read_hex_instead_of_legacy_entries(self):
        for binding in ['elixir', 'erlang']:
            with self.subTest(binding=binding):
                self.assertEqual(self.make_key(binding), TOKEN)

    def test_local_releases_keep_the_environment_override(self):
        for binding in ['elixir', 'erlang']:
            with self.subTest(binding=binding):
                self.assertEqual(self.make_key(binding, 'environment-key'),
                                 'environment-key')


if __name__ == '__main__':
    unittest.main()
