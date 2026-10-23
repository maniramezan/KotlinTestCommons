#!/usr/bin/env python3
"""Require tests for executable lines changed since a base revision.

Run after `./gradlew koverXmlReport` (per-module floors are enforced by `koverVerify`):

    python3 Scripts/check-coverage.py --diff-base origin/main

Reads each module's Kover XML report and the diff against the merge base, and fails when
fewer than DIFF_THRESHOLD percent of changed executable lines are covered. In GitHub Actions,
each uncovered changed line is annotated on the pull request.
"""

import argparse
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULES = ["core", "junit", "compose", "device"]

# Minimum coverage (percent) of executable lines changed relative to --diff-base.
DIFF_THRESHOLD = 90.0
# Modules whose changed lines need a device; keep their pure logic in tested internal functions.
DIFF_EXEMPT_MODULES = {"device"}
# Only production sources count; test sources are not measured.
MAIN_SOURCE = re.compile(r"^(?P<module>[^/]+)/src/[A-Za-z]*[Mm]ain/kotlin/(?P<path>.+\.kt)$")


def run(*command):
    return subprocess.run(command, cwd=ROOT, check=True, text=True, capture_output=True).stdout


def covered_lines():
    """Returns {(module, package path/File.kt): {line: covered}} from each module's Kover report."""
    result = {}
    for module in MODULES:
        report = ROOT / module / "build" / "reports" / "kover" / "report.xml"
        if not report.exists():
            raise SystemExit(f"Missing {report}; run `./gradlew koverXmlReport` first")
        for package in ElementTree.parse(report).getroot().iter("package"):
            for source in package.iter("sourcefile"):
                key = (module, f"{package.get('name')}/{source.get('name')}")
                lines = result.setdefault(key, {})
                for line in source.iter("line"):
                    number = int(line.get("nr"))
                    lines[number] = lines.get(number, False) or int(line.get("ci", "0")) > 0
    return result


def changed_lines(base):
    """Returns {(module, path): {line numbers}} added or modified since the merge base."""
    merge_base = run("git", "merge-base", base, "HEAD").strip()
    diff = run("git", "diff", "--unified=0", "--no-color", merge_base, "--", "*.kt")
    changes, current = {}, None
    for line in diff.splitlines():
        if line.startswith("+++ "):
            match = MAIN_SOURCE.match(line[6:]) if line.startswith("+++ b/") else None
            current = (match.group("module"), match.group("path")) if match else None
        elif line.startswith("@@") and current:
            start, _, count = re.search(r"\+(\d+)(,(\d+))?", line).groups()
            first, total = int(start), int(count) if count is not None else 1
            changes.setdefault(current, set()).update(range(first, first + total))
    return changes


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--diff-base", default="origin/main", help="Revision to diff against")
    args = parser.parse_args()

    coverage = covered_lines()
    executable, uncovered = 0, []
    for (module, path), lines in sorted(changed_lines(args.diff_base).items()):
        if module in DIFF_EXEMPT_MODULES:
            continue
        # Kover reports by package directory and file name; source sets for other platforms are absent.
        package_path = path.rsplit("/", 1)
        report_lines = coverage.get((module, "/".join(package_path)), {})
        for number in sorted(lines & report_lines.keys()):
            executable += 1
            if not report_lines[number]:
                uncovered.append((module, path, number))

    if executable == 0:
        print("No executable lines changed.")
        return
    percent = 100.0 * (executable - len(uncovered)) / executable
    print(f"Changed-line coverage: {percent:.1f}% of {executable} lines (minimum {DIFF_THRESHOLD}%)")
    for module, path, number in uncovered:
        file = next(ROOT.glob(f"{module}/src/*/kotlin/{path}"), Path(module) / path)
        relative = file.relative_to(ROOT) if file.is_absolute() else file
        if os.environ.get("GITHUB_ACTIONS") == "true":
            print(f"::warning file={relative},line={number}::Changed line is not covered by tests")
        else:
            print(f"  uncovered: {relative}:{number}")
    if percent < DIFF_THRESHOLD:
        sys.exit(1)


if __name__ == "__main__":
    main()
