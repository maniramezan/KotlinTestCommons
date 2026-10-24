# Independent Maven consumer verification

This is a separate Gradle build. It has no project dependency, composite build, source substitution, or
`mavenLocal()` repository. The TestCommons group resolves exclusively from the repository under test;
other dependencies use Google and Maven Central. External versions reuse the root version catalog.

| Consumer | Verification |
|---|---|
| `runtime` | Runs a JVM tool with only `core`, and checks that JUnit, kotlin-test, coroutines-test, Compose, and AndroidX Test classes are absent |
| `junit4` | Runs `MainDispatcherRule` without Jupiter on the classpath |
| `jupiter` | Runs `MainDispatcherExtension` with a consumer-supplied Jupiter engine |
| `multiplatform` | Runs core/matcher probes on JVM and Android host; compiles `device` use on Android and core/Compose use on both iOS architectures |

These are packaging and API-consumption checks. They do not launch an iOS Compose UI or an Android
instrumented consumer. Producer UI/device tests remain in the library and sample modules. See the
[compatibility and verification matrix](../../docs/compatibility.md).

Run these commands from the repository root on macOS with JDK 21, Android SDK 37, Xcode selected, Python 3, and curl.

## Verify a candidate before publication

```sh
candidate_version="0.0.0-local-verification"
./gradlew stageRelease -PVERSION_NAME="$candidate_version" -PRELEASE_SIGNING_ENABLED=false --no-configuration-cache
python3 Scripts/verify-release-artifacts.py --version "$candidate_version" --staging-repository build/release-staging --manifest build/release-manifest.json
./gradlew -p verification/consumer verifyConsumer -PtestCommonsVersion="$candidate_version" -PtestCommonsRepository="$PWD/build/release-staging"
```

`stageRelease` writes the same Maven publications used for Central into `build/release-staging`, with
signing disabled for this local verification. The manifest checks metadata, binaries, sources, and
documentation for all 13 publications. No credentials or external publication are involved.

CI uses a version containing the commit SHA so Gradle cannot reuse another commit's candidate under
the same coordinates. A release run instead stages the exact proposed stable version.

## Verify a published version

```sh
./gradlew -p verification/consumer verifyConsumer -PtestCommonsVersion=0.1.0 --refresh-dependencies
```

Omitting `testCommonsRepository` selects Maven Central. The version must be exact; ranges and dynamic
versions are rejected. For a just-published candidate, the release workflow first waits for every
manifest file to become downloadable, then runs this consumer build, and only then creates a tag.

Reports are under `verification/consumer/*/build/reports/`. Verification failures stop CI or release
progression. A skipped iOS UI test is not treated as runtime verification of that platform.
