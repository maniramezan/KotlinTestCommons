#!/usr/bin/env python3
"""Fail when a version pinned in the documentation differs from the latest release tag."""

import argparse
import re
import subprocess
import sys

RELEASE_TAG = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+")
PIN_PATTERNS = (
    re.compile(r"io\.github\.maniramezan\.testcommons:[a-z]+:(\d+\.\d+\.\d+)"),
    re.compile(r"testCommonsVersion\s*=\s*\"(\d+\.\d+\.\d+)\""),
    re.compile(r"-PtestCommonsVersion=(\d+\.\d+\.\d+)"),
    re.compile(r"The example pins `(\d+\.\d+\.\d+)`"),
)


def find_pins(text):
    """Return (line number, version) for every documented version pin in the text."""
    pins = []
    for number, line in enumerate(text.splitlines(), start=1):
        for pattern in PIN_PATTERNS:
            pins.extend((number, match.group(1)) for match in pattern.finditer(line))
    return pins


def latest_tag(tags):
    """Return the highest bare-SemVer tag, or None when there is none."""
    releases = [tag.strip() for tag in tags if RELEASE_TAG.fullmatch(tag.strip())]
    if not releases:
        return None
    return max(releases, key=lambda tag: tuple(int(part) for part in tag.split(".")))


def git_lines(*arguments):
    """Run git and return its non-empty output lines."""
    output = subprocess.run(
        ["git", *arguments], check=True, capture_output=True, text=True,
    ).stdout
    return [line for line in output.splitlines() if line]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--expected", help="version every pin must match; defaults to the latest tag")
    args = parser.parse_args()
    expected = args.expected
    if expected is None:
        expected = latest_tag(git_lines("tag", "--list", "--sort=-v:refname"))
        if expected is None:
            print("No release tags found; run 'git fetch --tags' or pass --expected.", file=sys.stderr)
            return 1
    stale = 0
    for path in git_lines("ls-files", "*.md"):
        with open(path, encoding="utf-8") as document:
            pins = find_pins(document.read())
        for number, version in pins:
            if version != expected:
                print(f"{path}:{number}: pins {version}, expected {expected}", file=sys.stderr)
                stale += 1
    if stale:
        return 1
    print(f"All documentation pins match {expected}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
