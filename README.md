# KotlinTestCommons

Shared test helpers for Kotlin, Android, and Compose Multiplatform. This is the Kotlin counterpart of
[SwiftTestCommons](https://github.com/maniramezan/SwiftTestCommons).

| Artifact | Targets | Use it for | Add it to |
|---|---|---|---|
| `core` | JVM, Android, iOS | Spies, scripted fakes, a manual clock, async waits, flow observation, layout geometry, automation IDs, temporary files and fixtures. No test-framework imports. | Any test source set; can also ship in tools and apps |
| `junit` | JVM | `MainDispatcherRule` (JUnit 4) and `MainDispatcherExtension` (JUnit 5). | JVM and Android unit tests |
| `compose` | Android, desktop, iOS | Compose UI test waits, matchers, scrolling, assertions, geometry, a themed rule, device configuration overrides. | Compose UI tests |
| `device` | Android | Espresso waits and matchers, UiAutomator snapshots, gestures, verified text entry, launch, diagnostics, device settings. | Instrumented tests |

The library targets JVM 17 bytecode and Android minSdk 26. Built with Kotlin 2.4, AGP 9, and Compose
Multiplatform 1.12. Building this repository requires JDK 21 and Android SDK 37; iOS tests also require
macOS and Xcode.

Start with the [runnable sample apps](samples/README.md), the [testing guide](docs/testing-guide.md),
the [compatibility matrix](docs/compatibility.md), or browse the [API documentation](https://maniramezan.github.io/KotlinTestCommons/).

## Installation

Add `mavenCentral()` to your dependency repositories. Choose the artifacts for your test environment;
Android dependencies below use a regular Android application's source-set names.

```kotlin
dependencies {
    testImplementation("io.github.maniramezan.testcommons:core:0.1.0")
    testImplementation("io.github.maniramezan.testcommons:junit:0.1.0")
    androidTestImplementation("io.github.maniramezan.testcommons:compose:0.1.0")
    androidTestImplementation("io.github.maniramezan.testcommons:device:0.1.0")
}
```

The example pins `0.1.0`. Use a specific [release](https://github.com/maniramezan/KotlinTestCommons/releases). Tags are bare SemVer
(`0.1.0`), never `v0.1.0`. Before 1.0, a minor release can contain breaking changes.
For Kotlin Multiplatform source sets and JUnit Jupiter setup, see the [testing guide](docs/testing-guide.md).

## Dependency spies and fakes

`CallRecorder<T>` records arguments in arrival order. Recording never suspends, so it works from callbacks
on any thread:

```kotlin
val recorder = CallRecorder<String>()
val client = FakeClient(onRequest = recorder::record)
client.fetch("a")
assertEquals(listOf("a"), recorder.values())
```

`ScriptedResponder<Request, Response>` answers call *n* with outcome *n* and records requests. `hold(call)`
returns an `AsyncGate` that suspends that call so in-flight state can be asserted. Running out throws
`ScriptedResponder.Exhausted`, never a scripted error:

```kotlin
val responder = ScriptedResponder<String, Int>(listOf(Result.failure(TestError()), Result.success(42)))
val repository = Repository(fetch = responder::respond)
```

`ScriptedValues` hands out deterministic IDs or dates with `Fail`, `RepeatLast`, or `Fallback` exhaustion.
`TestError()` is a payload-free exception; all instances are equal.

## Time and async coordination

`ManualClock` is a `kotlin.time.Clock`, a `TimeSource.WithComparableMarks`, and a millisecond source
(`clock::currentTimeMillis`). Its `sleep` resumes only when a test advances it:

```kotlin
val clock = ManualClock()
val job = launch { debouncer.submit("query") }
clock.waitForSleepers(1)
clock.advanceBy(300.milliseconds)
```

For code that calls `delay` directly, prefer `runTest` and its virtual time.

`waitUntil` polls with a monotonic real-time deadline and throws `ObservationTimeout` with the last value:

```kotlin
val count = withContext(Dispatchers.Default) {
    waitUntil(2.seconds, observe = { model.count }) { it >= 3 }
}
```

Inside `runTest`, `delay` is virtual, so wait from `Dispatchers.Default` when the observed work runs on
another thread. `observeFlow` collects a flow with an event budget and a real-time budget, and reports
whether it matched, reached the limit, finished, or timed out. `AsyncGate` is a one-shot signal with a
`waiterCount` for establishing interleavings.

`MainDispatcherRule` (JUnit 4) and `MainDispatcherExtension` (JUnit 5) install a test dispatcher as
`Dispatchers.Main` and always restore it:

```kotlin
@get:Rule val mainDispatcher = MainDispatcherRule()

@Test fun loads() = runTest(mainDispatcher.dispatcher) {
    assertEquals("Ready", repository.load())
}
```

## Layout geometry and reading order

`Frame` is a rectangle in one coordinate space. `FrameGeometry` checks containment, overlap, alignment, and
gaps; `ReadingOrder` checks that items read in order on one row, left to right or right to left. Invalid
frames and tolerances never match. The `compose` and `device` artifacts build frames from semantics nodes
and accessibility nodes:

```kotlin
assertReadingOrder(onNodeWithTag("day-1"), onNodeWithTag("day-2"), ReadingDirection.RightToLeft)
```

## Automation identifiers

`AutomationId` builds the same identifiers on Android and iOS: `<namespace>.<screen>.<region>.<element>[.<key>]`,
with snake_case segments and a percent-encoded key. Keep the catalog in the app:

```kotlin
val ids = AutomationId("myapp")
Modifier.testTag(ids.make("home", "feed", "video_card", key = video.id))
```

## Temporary files and fixtures

On the JVM and Android, `TemporaryDirectory` creates a uniquely named directory and writes nested files;
paths escaping it are rejected. Cleanup is explicit with `remove()`. `FixtureDirectory` reads text or bytes
from a root folder or a test class's resources. `SnapshotReferenceDirectory` finds an existing reference
folder for a test class and returns `null` otherwise, so the snapshot library keeps its default.
`fixtureEnvironment` and `roundTrip` build environments and check serialization without global state.

## Compose UI tests

Waits work on `ComposeUiTest` (`runComposeUiTest`) and on JUnit 4 `ComposeTestRule`. The single-node waits
require nodes to be displayed:

```kotlin
@get:Rule val composeRule = ThemedComposeRule(theme = { content -> AppTheme(content = content) })

@Test fun showsFeed() {
    composeRule.setContent { HomeScreen() }
    composeRule.waitForText("New videos")
    composeRule.waitUntilGone(hasText("Loading"))
    composeRule.scrollToNode(hasTestTag("video_42")).assertMinimumTouchTarget()
}
```

- Waits: `waitUntilDisplayed`, `waitUntilGone`, `waitForText`, `waitForTag`, `waitForContentDescription`.
- State selection: `waitForAnyTag` returns the first existing tag in argument order, including off-screen nodes.
- Matchers: `hasTestTagStartingWith`, `onNodeWithRequiredContentDescription`.
- Scrolling: `scrollContainer` picks the largest container on an axis; `scrollByViewport` and `scrollToNode`
  use semantics actions, so they work with lazy lists.
- Assertions: `assertMinimumTouchTarget`, `replaceTextVerified`, `assertReadingOrder`, `assertNoOverlap`,
  `assertContainedIn`.
- `MainTestClock.advanceFrames(n)` steps animations frame by frame. `captureToPng()` returns PNG bytes on
  Android and desktop.

On Android, `deviceConfiguration` combines locale, layout direction, font scale, dark mode, and size
overrides:

```kotlin
DeviceConfigurationOverride(deviceConfiguration(locales = LocaleList("fa-IR"), fontScale = 2f)) {
    AppTheme { Calendar() }
}
```

## Instrumented tests

Espresso:

- `waitUntilDisplayed` and `waitUntilNotDisplayed` keep the main thread running while they wait.
- `hasResourceEntryName` matches views without the app's `R` class.
- `withIndex` picks one of several matches.
- `View.containsText` finds text inside `AndroidView` interop.

UiAutomator:

- `AccessibilitySnapshot.capture()` reads every window's tree in one pass, including Compose UI, other
  apps, and system dialogs. `findTapTarget` resolves Compose's split semantics to the clickable node.
- `uniqueObject` fails on ambiguous selectors.
- `setTextVerified` checks the field's text afterward.
- `scrollUntilVisible` makes bounded scrolls.
- `dragWithHold` performs a real long-press drag. `pressBackReliably` dispatches back even when Compose
  navigation emits no content-change event.
- `setAppLocales` and `setAnimationsEnabled` change device settings.

Launching and runners:

- `launchTargetApp()` and `UiDevice.launchPackage(...)` start an app in a fresh task.
- Subclass `InstrumentationArgsRunner` with an explicit set of argument names to copy `-e` arguments
  to system properties, then configure that subclass as the runner. `instrumentationArgument(name)`
  reads an argument directly.
- `DiagnosticsOnFailureRule` saves a screenshot and the window hierarchy when a test fails.

## Contributing

Work on a feature branch and merge through a pull request. Run
`git config --local core.hooksPath .githooks` once per checkout to block direct pushes to `main`.
Run these before opening a pull request (CI runs the same checks):

```sh
./gradlew check
./gradlew koverXmlReport && python3 Scripts/check-coverage.py --diff-base origin/main
./gradlew :device:connectedAndroidDeviceTest
```

`check` runs every target's tests (including the iOS simulator on macOS), ktlint, the public API check,
and each module's coverage floor. After an intended API change, run `./gradlew updateApi`. API docs are
built with `./gradlew dokkaGenerate`.

CI also stages Maven publications and runs the [independent consumer build](verification/consumer/README.md).
This checks the packaging used by external applications, beyond the samples' local project dependencies.

Conventional Commits determine release-candidate versions. Publishing requires an explicit manual
Release run with the reviewed version and commit; tags follow successful public Maven Central verification. See the
[release and secrets guide](docs/releasing.md). There is no `CHANGELOG.md`.
See [AGENTS.md](AGENTS.md) for conventions.

## License

MIT.
