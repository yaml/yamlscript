import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class AdaReviewTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.log = self.directory / 'calls.json'
        gh = self.directory / 'gh'
        gh.write_text('''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
log = Path(os.environ['ADA_GH_LOG'])
calls = json.loads(log.read_text()) if log.exists() else []
queries = sum(call[:2] == ['pr', 'list'] for call in calls)
calls.append(sys.argv[1:])
log.write_text(json.dumps(calls))
if sys.argv[1:3] == ['pr', 'list']:
    if os.environ.get('ADA_QUERY_EXIT'):
        sys.exit(int(os.environ['ADA_QUERY_EXIT']))
    responses = json.loads(os.environ['ADA_PR_RESPONSES'])
    print(responses[min(queries, len(responses) - 1)])
elif sys.argv[1:3] == ['pr', 'ready']:
    sys.exit(int(os.environ.get('ADA_READY_EXIT', '0')))
else:
    sys.exit(97)
''')
        gh.chmod(0o755)
        sleep = self.directory / 'sleep'
        sleep.write_text('#!/usr/bin/env python3\n')
        sleep.chmod(0o755)
        self.alr = self.directory / 'alr'
        self.alr.write_text('''#!/usr/bin/env python3
import os, sys
from pathlib import Path
with Path(os.environ['ADA_ALR_LOG']).open('a') as log:
    log.write(' '.join(sys.argv[1:]) + '\\n')
''')
        self.alr.chmod(0o755)

    def run_helper(self, responses, **extra):
        env = dict(os.environ, **extra)
        env.update(
            PATH=str(self.directory) + os.pathsep + env['PATH'],
            ADA_GH_LOG=str(self.log),
            ADA_PR_RESPONSES=json.dumps(responses),
        )
        result = subprocess.run(
            ['bash', str(ROOT / 'util/release-ada-ready'),
             'ingydotnet', '0.3.6'],
            env=env, capture_output=True, text=True,
        )
        calls = json.loads(self.log.read_text())
        return result, calls

    def test_marks_draft_ready(self):
        result, calls = self.run_helper(['2256 true'])
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(calls[-1], [
            'pr', 'ready', '2256', '--repo', 'alire-project/alire-index',
        ])

    def test_already_ready_succeeds_without_another_mutation(self):
        result, calls = self.run_helper(['2256 false'])
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(calls), 1)
        self.assertIn('already ready for review', result.stdout)

    def test_retries_until_the_new_pr_is_visible(self):
        result, calls = self.run_helper(['', '', '2256 true'])
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(calls), 4)
        self.assertEqual(calls[-1][:2], ['pr', 'ready'])

    def test_missing_pr_fails_after_five_queries(self):
        result, calls = self.run_helper([''])
        self.assertEqual(result.returncode, 1)
        self.assertEqual(len(calls), 5)
        self.assertIn('PR not found', result.stderr)

    def test_lookup_failure_is_reported(self):
        result, calls = self.run_helper([''], ADA_QUERY_EXIT='4')
        self.assertEqual(result.returncode, 4)
        self.assertEqual(len(calls), 1)

    def test_ready_failure_is_reported(self):
        result, _ = self.run_helper(['2256 true'], ADA_READY_EXIT='9')
        self.assertEqual(result.returncode, 9)

    def run_release(self, responses):
        alr_log = self.directory / 'alr.log'
        env = dict(os.environ)
        env.update(
            PATH=str(self.directory) + os.pathsep + env['PATH'],
            ADA_GH_LOG=str(self.log),
            ADA_ALR_LOG=str(alr_log),
            ADA_PR_RESPONSES=json.dumps(responses),
        )
        result = subprocess.run([
            'make', '-C', str(ROOT / 'ada'), 'release',
            '-o', 'test', '-o', 'build-doc', 'v=0.3.6',
            'ALR=' + str(self.alr), 'GH=' + str(self.directory / 'gh'),
            'GH-BIN=' + str(self.directory),
            'XDG_RUNTIME_DIR=' + str(self.directory / 'runtime'),
            'ADA-GITHUB-USER=fixture-user', 'ADA-GITHUB-TOKEN=fixture-token',
        ], env=env, capture_output=True, text=True)
        calls = json.loads(self.log.read_text())
        return result, calls, alr_log.read_text()

    def test_release_reuses_an_existing_pr(self):
        result, calls, alr_calls = self.run_release(['2256', '2256 false'])
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertNotIn('publish', alr_calls)
        self.assertEqual(len(calls), 2)

    def test_release_submits_then_marks_a_new_pr_ready(self):
        result, calls, alr_calls = self.run_release(['', '', '2256 true'])
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn('publish --manifest ada/alire.toml', alr_calls)
        self.assertEqual(calls[-1][:3], ['pr', 'ready', '2256'])


if __name__ == '__main__':
    unittest.main()
