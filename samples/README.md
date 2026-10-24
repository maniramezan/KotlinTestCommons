# Sample apps

These apps use the library projects directly, so they work before a Maven Central release. They are
included in the root build and have no publishing configuration. Run commands from the repository root.
They demonstrate behavior using project dependencies. The separate
[Maven consumer build](../verification/consumer/README.md) verifies published packaging; the
[verification matrix](../docs/compatibility.md) describes the platform coverage.

Use JDK 21 and install Android SDK platform 37. The Android app runs on API 26 or newer. Gradle keeps
the library and sample bytecode at Java 17. For all checks, including iOS, use macOS with Xcode selected.

| App | Run | What to read |
|---|---|---|
| [JVM cache walkthrough](jvm) | `./gradlew :samples:jvm:run` | [Cache](jvm/src/main/kotlin/io/github/maniramezan/testcommons/samples/jvm/GreetingCache.kt), [JUnit 4 tests](jvm/src/test/kotlin/io/github/maniramezan/testcommons/samples/jvm/GreetingCacheTest.kt), [Jupiter test](jvm/src/test/kotlin/io/github/maniramezan/testcommons/samples/jvm/GreetingCacheExtensionTest.kt) |
| [Android learning library](android) | `./gradlew :samples:android:installDebug`, then open **Test Commons** on the device | [Model tests](android/src/test/kotlin/io/github/maniramezan/testcommons/samples/android/CatalogModelTest.kt), [Compose test](android/src/androidTest/kotlin/io/github/maniramezan/testcommons/samples/android/CatalogScreenTest.kt), [UiAutomator test](android/src/androidTest/kotlin/io/github/maniramezan/testcommons/samples/android/CatalogDeviceTest.kt) |

The JVM app demonstrates a cache hit and expiry using `ManualClock` and `ScriptedResponder`, without
waiting for real time. Its tests also demonstrate an in-flight request held by a gate, a retry after
failure, `CallRecorder`, and both dispatcher adapters. The sample cache is confined to one coroutine
context; it is not a concurrent cache implementation.

The Android app loads a local list of 40 lessons, lets you save a name, and supports reloading. It makes
no network requests. Its model accepts an injected loader so tests can hold or fail a request. Compose
tests use `ThemedComposeRule`, verified text replacement, `waitForTag`, touch-target assertions, and
lazy-list scrolling. The device test launches the actual app and uses the same `AutomationId` values
through UiAutomator. The screen enables `testTagsAsResourceId`; IDs remain separate from spoken labels.

Run the host tests and build the Android app:

```sh
./gradlew :samples:jvm:test :samples:android:testDebugUnitTest :samples:android:assembleDebug
```

With one emulator attached, run the Android sample tests:

```sh
./gradlew :samples:android:connectedDebugAndroidTest
```

CI builds both apps, runs the host tests through `check`, and runs the Android sample's Compose and
UiAutomator tests on API 35. Instrumented suites run sequentially so they do not compete for the same
emulator or accessibility connection. HTML reports are under each module's `build/reports` directory.
