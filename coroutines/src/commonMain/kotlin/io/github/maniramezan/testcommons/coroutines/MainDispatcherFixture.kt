package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Replaces `Dispatchers.Main` with [dispatcher] between [install] and [reset], from shared code.
 *
 * This is the multiplatform counterpart of the JUnit `MainDispatcherRule`, for tests in a
 * `commonTest` source set that run on iOS as well as the JVM. Pair the two calls with
 * `kotlin.test`'s `@BeforeTest` and `@AfterTest`:
 *
 * ```kotlin
 * private val main = MainDispatcherFixture()
 *
 * @BeforeTest fun setUp() = main.install()
 *
 * @AfterTest fun tearDown() = main.reset()
 *
 * @Test fun loads() = runTest(main.dispatcher) { ... }
 * ```
 *
 * Prefer [runTestWithMain] when one test body is all that needs the dispatcher.
 *
 * @property dispatcher The dispatcher installed as `Dispatchers.Main`.
 */
public class MainDispatcherFixture(public val dispatcher: TestDispatcher = StandardTestDispatcher()) {
    /** Installs [dispatcher] as `Dispatchers.Main`. */
    public fun install() {
        Dispatchers.setMain(dispatcher)
    }

    /** Restores the platform's `Dispatchers.Main`. Safe to call when [install] was not called. */
    public fun reset() {
        Dispatchers.resetMain()
    }
}
