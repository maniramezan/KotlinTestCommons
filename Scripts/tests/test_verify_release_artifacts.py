import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch

SPEC = importlib.util.spec_from_file_location(
    "verify_release_artifacts", Path(__file__).parents[1] / "verify-release-artifacts.py",
)
RELEASE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RELEASE)
VERSION = "0.1.1"


class PublicationManifestTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        for artifact in RELEASE.ARTIFACTS:
            binary = ".klib" if "-ios" in artifact else ".aar" if artifact.endswith("-android") else ".jar"
            folder = self.root / RELEASE.GROUP_PATH / artifact / VERSION
            folder.mkdir(parents=True)
            for suffix in (".pom", ".module", binary, "-sources.jar", "-javadoc.jar"):
                (folder / f"{artifact}-{VERSION}{suffix}").write_bytes(b"artifact")

    def write_manifest(self, manifest):
        path = self.root / "manifest.json"
        path.write_text(json.dumps(manifest))
        return path

    def test_records_all_variants_sources_and_documentation(self):
        manifest = RELEASE.collect_manifest(self.root, VERSION)
        self.assertEqual(65, len(manifest["files"]))
        self.assertEqual(VERSION, manifest["version"])
        self.assertEqual(
            manifest["files"], RELEASE.read_manifest(self.write_manifest(manifest), VERSION),
        )

    def test_rejects_an_incomplete_manifest(self):
        manifest = RELEASE.collect_manifest(self.root, VERSION)
        manifest["files"].pop()
        with self.assertRaisesRegex(ValueError, "every target"):
            RELEASE.read_manifest(self.write_manifest(manifest), VERSION)

    def test_rejects_duplicate_files_and_non_object_manifests(self):
        manifest = RELEASE.collect_manifest(self.root, VERSION)
        manifest["files"].append(manifest["files"][0])
        with self.assertRaisesRegex(ValueError, "duplicate"):
            RELEASE.read_manifest(self.write_manifest(manifest), VERSION)
        with self.assertRaisesRegex(ValueError, "object"):
            RELEASE.read_manifest(self.write_manifest([]), VERSION)

    def test_rejects_a_missing_platform_artifact(self):
        path = self.root / RELEASE.GROUP_PATH / "core-iosarm64" / VERSION / f"core-iosarm64-{VERSION}.klib"
        path.unlink()
        with self.assertRaisesRegex(ValueError, "Missing staged publication"):
            RELEASE.collect_manifest(self.root, VERSION)

    def test_rejects_an_unverified_new_target(self):
        folder = self.root / RELEASE.GROUP_PATH / "core-newtarget" / VERSION
        folder.mkdir(parents=True)
        (folder / f"core-newtarget-{VERSION}.pom").write_text("pom")
        with self.assertRaisesRegex(ValueError, "targets changed"):
            RELEASE.collect_manifest(self.root, VERSION)

    def test_rejects_missing_sources(self):
        path = self.root / RELEASE.GROUP_PATH / "junit" / VERSION / f"junit-{VERSION}-sources.jar"
        path.unlink()
        with self.assertRaisesRegex(ValueError, "sources.jar"):
            RELEASE.collect_manifest(self.root, VERSION)

    def test_ignores_other_versions_and_checksums(self):
        checksum = self.root / RELEASE.GROUP_PATH / "junit" / VERSION / f"junit-{VERSION}.jar.sha256"
        checksum.write_text("checksum")
        other = self.root / RELEASE.GROUP_PATH / "junit" / "0.1.0"
        other.mkdir()
        (other / "junit-0.1.0.jar").write_bytes(b"old")
        manifest = RELEASE.collect_manifest(self.root, VERSION)
        self.assertEqual(65, len(manifest["files"]))

    def test_rejects_a_manifest_for_another_version(self):
        path = self.write_manifest({"version": "0.1.0", "files": ["anything"]})
        with self.assertRaisesRegex(ValueError, "version"):
            RELEASE.read_manifest(path, VERSION)

    def test_rejects_empty_and_invalid_paths(self):
        for paths in ([], ["../private"], ["https://other.example/file"], [17]):
            with self.subTest(paths=paths):
                path = self.write_manifest({"version": VERSION, "files": paths})
                with self.assertRaises(ValueError):
                    RELEASE.read_manifest(path, VERSION)

    def test_rejects_other_namespaces_and_versions(self):
        for relative in (
            f"com/example/test/junit/{VERSION}/junit-{VERSION}.pom",
            f"{RELEASE.GROUP_PATH}/junit/0.1.0/junit-0.1.0.pom",
        ):
            with self.subTest(path=relative):
                path = self.write_manifest({"version": VERSION, "files": [relative]})
                with self.assertRaisesRegex(ValueError, "does not belong"):
                    RELEASE.read_manifest(path, VERSION)


class PublicationWaitTest(unittest.TestCase):
    def test_retries_only_missing_files(self):
        calls = []
        clock = [0]

        def probe(url):
            calls.append(url)
            return 200 if url.endswith("ready") or clock[0] > 0 else 404

        def advance(duration):
            clock[0] += duration

        self.assertTrue(RELEASE.wait_for_publication(
            "https://repo.example", ["ready", "later"], 5, 1,
            probe=probe, monotonic=lambda: clock[0], sleep=advance, report=lambda _: None,
        ))
        self.assertEqual(1, calls.count("https://repo.example/ready"))
        self.assertEqual(2, calls.count("https://repo.example/later"))

    def test_stops_at_the_deadline_with_missing_files_reported(self):
        clock = [0]
        output = []

        def advance(duration):
            clock[0] += duration

        self.assertFalse(RELEASE.wait_for_publication(
            "https://repo.example", ["absent"], 2, 10,
            probe=lambda _: 404, monotonic=lambda: clock[0], sleep=advance, report=output.append,
        ))
        self.assertEqual(2, clock[0])
        self.assertIn("  missing: absent", output)

    def test_zero_budget_makes_one_probe(self):
        self.assertFalse(RELEASE.wait_for_publication(
            "https://repo.example", ["absent"], 0, 1,
            probe=lambda _: 0, report=lambda _: None,
        ))

    def test_accepts_partial_content(self):
        self.assertTrue(RELEASE.wait_for_publication(
            "https://repo.example", ["present"], 0, 1,
            probe=lambda _: 206, report=lambda _: None,
        ))

    def test_fails_immediately_on_authorization_errors(self):
        with self.assertRaisesRegex(ValueError, "denied access"):
            RELEASE.wait_for_publication("https://repo.example", ["denied"], 0, 1, probe=lambda _: 403)

    def test_rejects_non_http_repositories(self):
        with self.assertRaisesRegex(ValueError, "HTTP"):
            RELEASE.wait_for_publication("file:///tmp/repository", ["artifact"], 0, 1)


class RepositoryProbeTest(unittest.TestCase):
    def test_head_falls_back_to_a_ranged_download(self):
        with patch.object(RELEASE.subprocess, "run", side_effect=[
            Mock(returncode=0, stdout="405"), Mock(returncode=0, stdout="206"),
        ]) as run:
            self.assertEqual(206, RELEASE.check_url("https://repo.example/artifact"))
        self.assertIn("--head", run.call_args_list[0].args[0])
        self.assertIn("--range", run.call_args_list[1].args[0])
        self.assertNotIn("--head", run.call_args_list[1].args[0])

    def test_reports_missing_files_and_network_errors(self):
        for response, expected in (
            (Mock(returncode=0, stdout="404"), 404),
            (Mock(returncode=28, stdout="000"), 0),
        ):
            with self.subTest(response=response):
                with patch.object(RELEASE.subprocess, "run", return_value=response):
                    self.assertEqual(expected, RELEASE.check_url("https://repo.example/artifact"))

    def test_tls_errors_fail_without_waiting_for_propagation(self):
        with patch.object(RELEASE.subprocess, "run", return_value=Mock(returncode=60, stdout="000")):
            with self.assertRaisesRegex(ValueError, "TLS certificate"):
                RELEASE.check_url("https://repo.example/artifact")


if __name__ == "__main__":
    unittest.main()
