# Compatibility and verification

The published library targets JVM 17 bytecode and Android minSdk 26. Building this repository uses
JDK 21, Android SDK 37, and the checked-in Gradle 9.8.2 wrapper. Native compilation and simulator tests
require macOS with Xcode selected.

The following versions are tested together. This is a tested configuration, not a claim that every
older or newer toolchain is compatible.

| Component | Tested version |
|---|---|
| Kotlin | 2.4.21 |
| Android Gradle Plugin | 9.4.1 |
| kotlinx.coroutines | 1.11.0 |
| Compose Multiplatform | 1.12.2 |
| AndroidX Compose test manifest | 1.12.1 |
| JUnit 4 | 4.13.2 |
| JUnit Jupiter | 6.1.3 |
| Android instrumented-test emulator | API 35 |

The [version catalog](../gradle/libs.versions.toml) pins all dependency and plugin versions. JUnit's
Jupiter extension API is `compileOnly` in `junit`; a Jupiter consumer supplies its API, engine, and
launcher. A JUnit 4 consumer does not need Jupiter. Android applications using AGP 9 use its built-in
Kotlin support.

## What CI verifies

| Artifact/target | Producer behavior checks | Independent Maven consumer checks |
|---|---|---|
| `core` JVM | Unit tests, including concurrency and real-time observation | Runs a tool without test-framework dependencies |
| `core` Android | Host unit tests | Compiles and runs host probes |
| `core` iOS simulator | Common unit tests on the simulator | Compiles the simulator variant |
| `core` iOS device | Compilation and ABI validation | Compiles the device variant |
| `junit` JVM | JUnit 4 and Jupiter adapter tests | Runs both frameworks in separate consumers; Jupiter is absent from the JUnit 4 consumer |
| `junit` Android | Used by sample host tests | Runs the rule in Android host tests |
| `coroutines` JVM, Android, iOS | Common tests on every target, including the iOS simulator | Runs `runTestWithMain` from common tests on every consumer target |
| `compose` JVM/desktop | UI tests with the host renderer | Loads core/matcher APIs from the published JVM variant |
| `compose` Android | Robolectric helpers and sample instrumented UI tests | Compiles the Android variant and loads matcher APIs in host tests |
| `compose` iOS | Compilation and ABI validation | Compiles both native variants |
| `device` Android | Pure-logic host tests and API 35 instrumented tests | Compiles an actual UiAutomator query using the published Android variant |

**iOS Compose UI interaction tests do not currently run in CI.** Publishing an iOS variant and compiling
a consumer do not verify rendering, input, scrolling, or UI-test execution on iOS. An iOS app should
validate those workflows in its own test host. Adding an iOS UI test host is future work, not an
existing guarantee.

Android's declared minimum is API 26; the instrumented suite currently executes on API 35. The sample
also exercises a short window with 200% font scale. This does not constitute a full device/OS matrix.

## Using the library

Use the base Gradle coordinates (`core`, `junit`, `coroutines`, `compose`, `device`); Gradle selects the appropriate
published variant. Install helpers in test configurations except when deliberately shipping `core`
in a tool. See the [testing guide](testing-guide.md) for KMP source sets and runner configuration.

The [consumer build](../verification/consumer/README.md) checks staged Maven publications before release
and public Maven Central downloads after publication. The runnable samples use local project dependencies
and demonstrate behavior; they are not a substitute for this packaging check.
