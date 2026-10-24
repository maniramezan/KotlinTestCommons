# Testing guide

Choose dependencies according to where tests run. `core` contains no test framework and can also be
used in development tools. `junit`, `compose`, and `device` belong in test configurations.
See the [compatibility and verification matrix](compatibility.md) for tested versions and platform limits.

## Kotlin Multiplatform

With `mavenCentral()` configured, add the dependencies to the owning source sets. Pin
`testCommonsVersion` to your chosen [release](https://github.com/maniramezan/KotlinTestCommons/releases).
This example assumes a project with a JVM target; omit `jvmTest` when that target is absent.

```kotlin
val testCommonsVersion = "0.1.0"

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.github.maniramezan.testcommons:core:$testCommonsVersion")
            implementation("io.github.maniramezan.testcommons:compose:$testCommonsVersion")
        }
        jvmTest.dependencies {
            implementation("io.github.maniramezan.testcommons:junit:$testCommonsVersion")
        }
    }
}
```

Add `compose` only if your shared tests exercise Compose UI. For common UI tests, use
`androidx.compose.ui.test.v2.runComposeUiTest` and opt in to `androidx.compose.ui.test.ExperimentalTestApi`
where required. `ComposeTestRule` and `ThemedComposeRule` are available on JVM and Android. Device
configuration overrides are Android-only; image capture is available on Android and desktop.

In an Android KMP library using AGP 9, enable `withHostTest {}` and `withDeviceTest {}` on the Android
target as needed. Put `junit` in `androidHostTest` and `device` in `androidDeviceTest`, not `commonTest`.
Use `androidx.test.runner.AndroidJUnitRunner` as the instrumentation runner. The library's
[device module](../device/build.gradle.kts) shows this setup.

## Android applications

A regular Android app uses `testImplementation` for local tests and `androidTestImplementation` for
instrumented tests. AGP 9 provides Kotlin support directly; do not add the Kotlin Android plugin.
The [Android sample build](../samples/android/build.gradle.kts) pins the dependencies in the shared
version catalog and demonstrates the complete configuration.

Compose tests that use `createComposeRule()` or the default `ThemedComposeRule` need the
`androidx.compose.ui:ui-test-manifest` artifact in `debugImplementation`. Tests using
`createAndroidComposeRule<YourActivity>()` instead use the app's own activity. Pass that rule as
`ThemedComposeRule`'s `delegate` only when the activity leaves its content for the test to install;
an activity that already calls `setContent` should be tested through its existing content.

UiAutomator sees Compose test tags when an ancestor enables resource IDs:

```kotlin
val ids = AutomationId("sample")
Column(Modifier.semantics { testTagsAsResourceId = true }) {
    BasicText("Ready", Modifier.testTag(ids.make("home", "status", "ready")))
}
```

Select a tag with `By.res(fullTag)`; use the single-argument overload because the tag is already the
complete resource name. Give interactive controls meaningful spoken labels independently of their
tags. See the [sample device test](../samples/android/src/androidTest/kotlin/io/github/maniramezan/testcommons/samples/android/CatalogDeviceTest.kt).

For app configuration supplied through instrumentation arguments, `instrumentationArgument(name)`
reads a nonempty value directly. To expose selected arguments through `System.getProperty` before the
app starts, subclass `InstrumentationArgsRunner` with the allowed names and configure that subclass as
`testInstrumentationRunner`. The base runner requires this explicit selection.

## JUnit Jupiter

`MainDispatcherExtension` uses Jupiter's extension API. That API is `compileOnly` in the library, so
consumers must add their own Jupiter dependencies and enable the JUnit Platform. The
[JVM sample build](../samples/jvm/build.gradle.kts) demonstrates Jupiter and Vintage together; Vintage
is needed only when running JUnit 4 tests through the same platform.

Register the extension as an instance field with `@JvmField` and `@RegisterExtension`, and pass
`mainDispatcher.dispatcher` to `runTest`. See the
[complete Jupiter example](../samples/jvm/src/test/kotlin/io/github/maniramezan/testcommons/samples/jvm/GreetingCacheExtensionTest.kt).
Both dispatcher adapters change global `Dispatchers.Main`; avoid running tests that replace Main in
parallel within one JVM.

## Time, gates, and flows

`runTest` advances a coroutine test scheduler. `ManualClock` advances only injected time and its own
sleepers. Advancing either one does not advance the other. Use `runCurrent()` to establish a suspended
request or sleeper on the test scheduler before releasing a gate or advancing the manual clock.

`waitUntil`, `CallRecorder.waitForCalls`, `ScriptedResponder.waitForCalls`, and
`ManualClock.waitForSleepers` measure real time but suspend with `delay`. When called from `runTest`
while work happens on a real dispatcher, wrap the wait in `withContext(Dispatchers.Default)` to avoid
spinning through virtual delays. For work scheduled entirely on the test scheduler, use `runCurrent`,
`advanceTimeBy`, or `advanceUntilIdle` instead.

Register `ScriptedResponder.hold(call)` before starting that call. Its zero-based call index determines
the response even when held calls finish out of order. Cancellation removes the gate waiter and
propagates to the caller. Prefer `Result.failure(TestError())` for a scripted failure; an unexpected
extra request throws `ScriptedResponder.Exhausted` and cannot be mistaken for that scripted error.

`observeFlow` starts its real-time budget before collection, even for synchronous upstream work, and
keeps collection on the caller's dispatcher. Always assert the stopping reason as well as values:

```kotlin
val observation = observeFlow(flowOf("Loading", "Ready"), maxCount = 5, timeout = 2.seconds) {
    it == "Ready"
}
assertEquals(listOf("Loading", "Ready"), observation.values)
assertEquals(FlowObservation.End.MATCHED, observation.end)
```

The budget uses cooperative cancellation; it cannot interrupt arbitrary blocking code. An event
predicate takes precedence over the event limit when the final allowed event also matches.

## Visibility, scrolling, and diagnostics

`waitForTag`, `waitForText`, and `waitUntilDisplayed` require a displayed node. `waitUntilGone` also
succeeds when matching nodes exist off screen. `waitForAnyTag` intentionally checks existence in the
unmerged tree and returns the first candidate tag in argument order. Follow it with `waitForTag` if
the selected state must also be visible.

For an item that has not yet been composed in a lazy list, use `scrollToNode`. Pass an explicit
container when several lists could contain the target. UiAutomator's `scrollUntilVisible` checks
before scrolling and after every attempt, including the final attempt. A false scroll result can mean
no accessibility event arrived; the helper continues within its attempt budget. A nonpositive budget
performs a lookup without scrolling. An explicit missing container ends the search.
Gestures use a 20% inset from container edges so small screens do not send a return scroll into system
bars or app chrome.

`DiagnosticsOnFailureRule` saves a screenshot and hierarchy in the instrumentation package's external
files directory, under `test-diagnostics/<TestClass>/`. Pull that directory before uninstalling the
test APK. Reports and per-test logcat from connected tests are also uploaded by CI. Password replacement
verification can detect a changed mask, but cannot prove the underlying secret; assert the resulting
app behavior when the exact value matters.

## Contributing

Work on a feature branch and configure the push guard with `git config --local core.hooksPath .githooks`.
Run the commands below before opening a PR. The first two include tests, style, ABI checks, coverage
floors, and changed-line coverage. The device commands require an emulator and run sequentially.

```sh
./gradlew check
./gradlew koverXmlReport
python3 Scripts/check-coverage.py --diff-base origin/main
./gradlew :device:connectedAndroidDeviceTest
./gradlew :samples:android:connectedDebugAndroidTest
```

After an intentional public API change, run `./gradlew updateApi` and include the API dumps. Generate API
docs with `./gradlew dokkaGenerate`. Samples are not published and do not affect the library coverage
floors. Put migration notes in PR descriptions and release notes on GitHub Releases.

The [independent consumer build](../verification/consumer/README.md) verifies staged publications through
Maven coordinates before publishing and verifies public downloads afterward. For publishing credentials,
release preparation, and safe retries, see the [release guide](releasing.md).
