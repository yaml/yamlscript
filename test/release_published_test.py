import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class PowerShellPublicationTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        curl = self.directory / 'curl'
        curl.write_text('''#!/usr/bin/env python3
import sys

url = sys.argv[-1]
if url.startswith('https://www.powershellgallery.com/api/v2/Packages('):
    if url.endswith("Version='1.2.3')"):
        # The metadata endpoint returns 404 for HEAD, even when published.
        sys.exit(22 if any('I' in arg for arg in sys.argv[1:-1]) else 0)
    sys.exit(22)
if url.startswith('https://www.powershellgallery.com/packages/YAMLScript/'):
    # Missing Gallery pages redirect to an older release with HTTP 200.
    sys.exit(0)
sys.exit(97)
''')
        curl.chmod(0o755)

    def check_version(self, version, quiet=False):
        env = dict(os.environ)
        env.update(
            PATH=str(self.directory) + os.pathsep + env['PATH'],
            YS_RELEASE_PUBLISHED_QUIET='1' if quiet else '',
        )
        return subprocess.run(
            ['bash', str(ROOT / 'util/release-binding-published'),
             'powershell', version],
            env=env, capture_output=True, text=True,
        )

    def test_existing_version_is_published(self):
        result = self.check_version('1.2.3')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout, 'Published: powershell 1.2.3\n')

    def test_missing_version_is_not_published_despite_gallery_redirect(self):
        result = self.check_version('9.9.9')
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, 'Not published: powershell 9.9.9\n')

    def test_quiet_publication_check_preserves_the_result(self):
        for version, expected in [('1.2.3', 0), ('9.9.9', 1)]:
            with self.subTest(version=version):
                result = self.check_version(version, quiet=True)
                self.assertEqual(result.returncode, expected)
                self.assertEqual(result.stdout, '')


if __name__ == '__main__':
    unittest.main()
