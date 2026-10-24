#!/usr/bin/env python3
"""Record staged publications or wait until their files can be downloaded from Maven Central."""

import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import os
from pathlib import Path, PurePosixPath
import re
import sys
import subprocess
import time
from urllib.parse import urlparse

GROUP_PATH = "io/github/maniramezan/testcommons"
ARTIFACTS = (
    "core", "core-jvm", "core-android", "core-iosarm64", "core-iossimulatorarm64",
    "junit", "coroutines", "coroutines-jvm", "coroutines-android", "coroutines-iosarm64",
    "coroutines-iossimulatorarm64", "compose", "compose-jvm", "compose-android", "compose-iosarm64",
    "compose-iossimulatorarm64", "device", "device-android",
)
VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?")
PUBLICATION_SUFFIXES = {".pom", ".module", ".jar", ".aar", ".klib"}


def publication_paths(artifact, version):
    """Return the minimum files required for one published target."""
    prefix = f"{GROUP_PATH}/{artifact}/{version}/{artifact}-{version}"
    binary = ".klib" if "-ios" in artifact else ".aar" if artifact.endswith("-android") else ".jar"
    return [prefix + suffix for suffix in (".pom", ".module", binary, "-sources.jar", "-javadoc.jar")]


def collect_manifest(repository, version):
    """Require every publication's metadata, primary artifact, sources, and documentation."""
    group = repository / GROUP_PATH
    found = {
        folder.name for folder in group.iterdir()
        if folder.is_dir() and (folder / version / f"{folder.name}-{version}.pom").is_file()
    }
    if found != set(ARTIFACTS):
        raise ValueError(
            f"Publication targets changed; update consumer verification. "
            f"Missing: {sorted(set(ARTIFACTS) - found)}; unexpected: {sorted(found - set(ARTIFACTS))}"
        )
    paths = set()
    for artifact in ARTIFACTS:
        for relative in publication_paths(artifact, version):
            if not (repository / relative).is_file():
                raise ValueError(f"Missing staged publication file: {relative}")
        folder = repository / GROUP_PATH / artifact / version
        paths.update(
            path.relative_to(repository).as_posix()
            for path in folder.iterdir()
            if path.is_file() and path.suffix in PUBLICATION_SUFFIXES
        )
    return {"version": version, "files": sorted(paths)}


def read_manifest(path, version):
    """Read only paths belonging to the expected namespace and release version."""
    manifest = json.loads(path.read_text())
    if not isinstance(manifest, dict):
        raise ValueError("Manifest must be an object")
    if manifest.get("version") != version:
        raise ValueError("Manifest version does not match the requested release")
    paths = manifest.get("files")
    if not isinstance(paths, list) or not paths:
        raise ValueError("Manifest must contain publication files")
    for relative in paths:
        if not isinstance(relative, str) or not re.fullmatch(r"[A-Za-z0-9/_.-]+", relative):
            raise ValueError("Manifest contains an invalid path")
        parts = PurePosixPath(relative).parts
        if (
            ".." in parts
            or not relative.startswith(GROUP_PATH + "/")
            or len(parts) != 7
            or parts[-2] != version
            or parts[-3] not in ARTIFACTS
        ):
            raise ValueError(f"Manifest path does not belong to this release: {relative}")
    if len(paths) != len(set(paths)):
        raise ValueError("Manifest contains duplicate files")
    required = {path for artifact in ARTIFACTS for path in publication_paths(artifact, version)}
    if not required.issubset(paths):
        raise ValueError("Manifest must include every target's metadata, binary, sources, and documentation")
    return paths


def check_url(url):
    """Use curl's system TLS trust and fall back to a ranged GET when HEAD is unsupported."""
    common = [
        "curl", "--disable", "--silent", "--show-error", "--location", "--max-time", "10",
        "--output", os.devnull, "--write-out", "%{http_code}",
    ]
    for method in (["--head"], ["--range", "0-0"]):
        response = subprocess.run(common + method + [url], capture_output=True, text=True, check=False)
        if response.returncode == 60:
            raise ValueError("Repository TLS certificate verification failed; check the system CA trust")
        if response.returncode != 0:
            return 0
        status = int(response.stdout.strip())
        if status not in (405, 501):
            return status
    return status


def wait_for_publication(
    repository, paths, timeout, interval, probe=check_url,
    monotonic=time.monotonic, sleep=time.sleep, report=print,
):
    """Retry unavailable files within a real-time budget and retain successful probes."""
    parsed = urlparse(repository)
    if parsed.scheme not in ("https", "http") or not parsed.netloc:
        raise ValueError("Published repository must be an HTTP or HTTPS URL")
    deadline = monotonic() + timeout
    pending = set(paths)
    with ThreadPoolExecutor(max_workers=8) as executor:
        while pending:
            candidates = sorted(pending)
            urls = [repository.rstrip("/") + "/" + path for path in candidates]
            statuses = list(executor.map(probe, urls))
            for path, status in zip(candidates, statuses):
                if status in (200, 206):
                    pending.remove(path)
                elif status in (401, 403):
                    raise ValueError(f"Repository denied access ({status}): {path}")
            report(f"Publication files available: {len(paths) - len(pending)}/{len(paths)}")
            if not pending:
                return True
            remaining = deadline - monotonic()
            if remaining <= 0:
                report("Publication is not yet downloadable; do not create a release tag.")
                for path in sorted(pending):
                    report(f"  missing: {path}")
                return False
            sleep(min(interval, remaining))
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--staging-repository", type=Path)
    parser.add_argument("--repository", default="https://repo.maven.apache.org/maven2")
    parser.add_argument("--timeout-seconds", type=float, default=1800)
    parser.add_argument("--interval-seconds", type=float, default=20)
    args = parser.parse_args()
    if not VERSION.fullmatch(args.version):
        parser.error("version must be an exact version")
    if args.timeout_seconds < 0 or args.interval_seconds <= 0:
        parser.error("timeout must be nonnegative and interval must be positive")
    try:
        if args.staging_repository:
            manifest = collect_manifest(args.staging_repository, args.version)
            args.manifest.parent.mkdir(parents=True, exist_ok=True)
            args.manifest.write_text(json.dumps(manifest, indent=2) + "\n")
            print(f"Recorded {len(manifest['files'])} files across {len(ARTIFACTS)} publications")
            return 0
        paths = read_manifest(args.manifest, args.version)
        return 0 if wait_for_publication(
            args.repository, paths, args.timeout_seconds, args.interval_seconds,
        ) else 1
    except (ValueError, OSError) as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
