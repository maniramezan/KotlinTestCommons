import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location(
    "check_doc_versions", Path(__file__).parents[1] / "check-doc-versions.py",
)
CHECK = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHECK)


class FindPinsTest(unittest.TestCase):
    def test_finds_a_maven_coordinate(self):
        text = "dependencies {\n    testImplementation(\"io.github.maniramezan.testcommons:core:1.2.3\")\n}"
        self.assertEqual(CHECK.find_pins(text), [(2, "1.2.3")])

    def test_finds_a_version_property(self):
        text = "# Guide\n\nval testCommonsVersion = \"4.5.6\""
        self.assertEqual(CHECK.find_pins(text), [(3, "4.5.6")])

    def test_finds_a_gradle_property(self):
        text = "./gradlew verifyConsumer -PtestCommonsVersion=7.8.9 --refresh-dependencies"
        self.assertEqual(CHECK.find_pins(text), [(1, "7.8.9")])

    def test_finds_the_example_sentence(self):
        text = "Intro\nThe example pins `0.3.0`. Use a specific release."
        self.assertEqual(CHECK.find_pins(text), [(2, "0.3.0")])

    def test_ignores_notation_examples(self):
        self.assertEqual(CHECK.find_pins("Tags are bare SemVer (`0.1.0`), never `v0.1.0`."), [])


class ManifestVersionTest(unittest.TestCase):
    def test_reads_the_root_package_version(self):
        self.assertEqual(CHECK.manifest_version('{\n  ".": "0.3.0"\n}\n'), "0.3.0")

    def test_rejects_a_missing_root_package(self):
        with self.assertRaises(ValueError):
            CHECK.manifest_version('{"core": "0.3.0"}')

    def test_rejects_a_prefixed_or_prerelease_version(self):
        for version in ("v0.3.0", "0.3.0-rc1"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                CHECK.manifest_version(f'{{".": "{version}"}}')


if __name__ == "__main__":
    unittest.main()
