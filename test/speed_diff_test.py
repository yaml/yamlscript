"""Regression tests for committed-source comparisons and output checking."""

import contextlib
import importlib.util
import io
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


SPEC = importlib.util.spec_from_file_location(
    "speed_diff", Path(__file__).resolve().parents[1] / "util/speed-diff.py")
speed = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(speed)


class SnapshotTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        self.git("init", "--quiet")
        self.git("config", "user.name", "Test")
        self.git("config", "user.email", "test@example.com")
        self.file = self.repo / "source"
        self.file.write_text("first\n")
        self.git("add", "source")
        self.git("commit", "--quiet", "-m", "First")
        self.file.write_text("second\n")
        self.git("commit", "--quiet", "-am", "Second")

    def git(self, *args):
        return speed.run(["git", "-C", str(self.repo), *args])

    def test_snapshots_exclude_dirty_and_untracked_files(self):
        self.file.write_text("dirty\n")
        extra = self.repo / "extra"
        extra.write_text("untracked\n")
        before = self.git("status", "--porcelain")
        old, _ = speed.snapshot(self.repo, "HEAD^", self.root / "cache")
        new, identity = speed.snapshot(self.repo, "HEAD", self.root / "cache")
        self.assertEqual((old / "source").read_text(), "first\n")
        self.assertEqual((new / "source").read_text(), "second\n")
        self.assertFalse((new / "extra").exists())
        self.assertEqual(self.git("status", "--porcelain"), before)
        self.assertEqual(speed.snapshot(self.repo, "HEAD", self.root / "cache"),
                         (new, identity))
        (new / "source").write_text("modified cache\n")
        with self.assertRaisesRegex(speed.SpeedError, "Cached source was modified"):
            speed.snapshot(self.repo, "HEAD", self.root / "cache")
        self.assertEqual((new / "source").read_text(), "modified cache\n")

    def test_invalid_revision_does_not_change_working_tree(self):
        before = self.git("rev-parse", "HEAD")
        with self.assertRaises(speed.SpeedError):
            speed.snapshot(self.repo, "missing-revision", self.root / "cache")
        self.assertEqual(self.git("rev-parse", "HEAD"), before)
        self.assertFalse((self.root / "cache").exists())

    def test_cache_lock_rejects_overlap_and_releases_after_failure(self):
        cache = self.root / "cache"
        with self.assertRaisesRegex(ValueError, "interrupted"):
            with speed.cache_lock(cache):
                with self.assertRaisesRegex(speed.SpeedError, "Cache is in use"):
                    with speed.cache_lock(cache):
                        self.fail("overlapping comparison was allowed")
                raise ValueError("interrupted")
        with speed.cache_lock(cache):
            self.assertTrue((cache / ".lock").is_dir())
        self.assertFalse((cache / ".lock").exists())


class OutputTest(unittest.TestCase):
    def test_compilation_ignores_only_source_formatting(self):
        case = {"format": "clojure"}
        left = b'(let [x, "hello ; world"] ; comment\n [x \\; \\space])'
        right = b'(let\n[x "hello ; world"] [x \\; \\space])'
        self.assertEqual(speed.normalized_output(left, case),
                         speed.normalized_output(right, case))
        self.assertNotEqual(speed.normalized_output(b'"a b"', case),
                            speed.normalized_output(b'"a  b"', case))
        self.assertNotEqual(speed.normalized_output(b'(a b)', case),
                            speed.normalized_output(b'(ab)', case))
        self.assertNotEqual(speed.normalized_output(b'\\space', case),
                            speed.normalized_output(b'\\tab', case))

    def test_execution_output_requires_identical_bytes(self):
        self.assertNotEqual(speed.normalized_output(b"[1 2]", {}),
                            speed.normalized_output(b"[1, 2]", {}))

    def measure(self, command):
        binaries = {name: sys.executable for name in ("old", "new", "graalvm")}
        with contextlib.redirect_stdout(io.StringIO()):
            return speed.measure(binaries, [{"name": "test", "args": command}],
                                 runs=2, warmups=1, env=None)

    def test_samples_and_ratios(self):
        rows = self.measure(["-c", "print('same output')"])
        row = rows[0]
        for name in ("old", "new", "graalvm"):
            self.assertEqual(len(row["seconds"][name]), 2)
            self.assertGreater(row["medians"][name], 0)
        self.assertEqual(row["speedup"],
                         row["medians"]["old"] / row["medians"]["new"])
        self.assertEqual(len(set(row["stdout_sha256"].values())), 1)

    def test_nonzero_exit_fails(self):
        with self.assertRaisesRegex(speed.SpeedError, "failed for old"):
            self.measure(["-c", "raise SystemExit(2)"])

    def test_output_mismatch_fails(self):
        with self.assertRaisesRegex(speed.SpeedError, "Output differs"):
            self.measure(["-c", "import os; print(os.getpid())"])

    def test_timeout_has_case_and_runtime(self):
        with patch.object(speed.subprocess, "run", side_effect=
                          subprocess.TimeoutExpired("test", 60)):
            with self.assertRaisesRegex(speed.SpeedError,
                                        "test timed out for old"):
                self.measure(["-c", "pass"])


class BuildCacheTest(unittest.TestCase):
    def test_reuse_requires_matching_revision_and_binary_hash(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            ys, source = root / "ys", root / "glojure"
            ys.mkdir()
            (ys / "Meta").write_text("version: 0.3.5\n")
            oracle = root / "graalvm"
            oracle.write_bytes(b"bootstrap")
            go_env = dict(GOVERSION="go1.24.0", GOOS="linux", GOARCH="amd64",
                          GOROOT=str(root), GOPATH=str(root),
                          GOMODCACHE=str(root), GOCACHE=str(root))

            def build(command, **kwargs):
                if command[0] == "make":
                    binary = Path(next(arg.split("=", 1)[1] for arg in command
                                       if arg.startswith("CLI-BIN-GLOJURE=")))
                    binary.write_bytes(b"compiled binary")
                return subprocess.CompletedProcess(command, 0)

            def invoke(revision="old", rebuild=False, env=None):
                return speed.build_runtime(source, revision, ys, "ys-head",
                                           root / "gloat", "gloat-head", oracle,
                                           go_env, root / "cache", env or {},
                                           rebuild)

            with patch.object(speed.subprocess, "run", side_effect=build) as mock:
                with patch.object(speed, "run", return_value=str(source)):
                    with contextlib.redirect_stdout(io.StringIO()):
                        binary, metadata = invoke()
                        self.assertEqual(mock.call_count, 2)
                        self.assertEqual(invoke(), (binary, metadata))
                        self.assertEqual(mock.call_count, 2)
                        binary.write_bytes(b"tampered")
                        invoke()
                        self.assertEqual(mock.call_count, 4)
                        other_binary, _ = invoke("new")
                        self.assertNotEqual(binary, other_binary)
                        self.assertEqual(mock.call_count, 6)
                        invoke(rebuild=True)
                        self.assertEqual(mock.call_count, 8)
                        flags = {"GOFLAGS": "-gcflags=all=-l"}
                        flagged_binary, metadata = invoke(env=flags)
                        self.assertNotEqual(binary, flagged_binary)
                        self.assertEqual(mock.call_count, 10)
                        self.assertEqual(metadata["identity"]["go_flags"],
                                         flags["GOFLAGS"])
                        self.assertEqual(mock.call_args.kwargs["env"]["GOFLAGS"],
                                         flags["GOFLAGS"])
                        self.assertEqual(invoke(env=flags),
                                         (flagged_binary, metadata))
                        self.assertEqual(mock.call_count, 10)


if __name__ == "__main__":
    unittest.main()
