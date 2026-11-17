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


class LatestTagTest(unittest.TestCase):
    def test_sorts_numerically_and_skips_other_tags(self):
        self.assertEqual(CHECK.latest_tag(["0.9.0", "v1.0.0", "1.0.0-rc1", "0.10.0", "list"]), "0.10.0")

    def test_returns_none_without_release_tags(self):
        self.assertIsNone(CHECK.latest_tag([]))


if __name__ == "__main__":
    unittest.main()
