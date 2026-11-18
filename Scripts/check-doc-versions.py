#!/usr/bin/env python3
"""Fail when a version pinned in the documentation differs from the release-please manifest."""

import argparse
import json
import re
import subprocess
import sys

RELEASE_VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+")
MANIFEST = ".release-please-manifest.json"
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


def manifest_version(text):
    """Return the root package's bare-SemVer version from release-please manifest text.

    The release PR raises the manifest and the documented pins together, so they match on the PR,
    on `main` before the release is tagged, and after it.
    """
    version = json.loads(text).get(".")
    if not isinstance(version, str) or not RELEASE_VERSION.fullmatch(version):
        raise ValueError(f"{MANIFEST} must map \".\" to a bare SemVer version, found {version!r}")
    return version


def git_lines(*arguments):
    """Run git and return its non-empty output lines."""
    output = subprocess.run(
        ["git", *arguments], check=True, capture_output=True, text=True,
    ).stdout
    return [line for line in output.splitlines() if line]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--expected", help=f"version every pin must match; defaults to the {MANIFEST} version")
    args = parser.parse_args()
    expected = args.expected
    if expected is None:
        try:
            with open(MANIFEST, encoding="utf-8") as manifest:
                expected = manifest_version(manifest.read())
        except (OSError, ValueError) as error:
            print(f"Cannot read the release version: {error}", file=sys.stderr)
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
