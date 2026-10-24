package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration

/**
 * Runs [testBody] like `runTest` with [dispatcher] installed as `Dispatchers.Main`, and restores
 * the platform's main dispatcher afterward, including when the body fails.
 *
 * The test body and the code under test share [dispatcher]'s scheduler, so `advanceUntilIdle`
 * and virtual time cover work launched on `Dispatchers.Main`, such as a view model's scope.
 *
 * ```kotlin
 * @Test fun loads() = runTestWithMain {
 *     val model = ProfileModel()
 *     advanceUntilIdle()
 *     assertEquals("Ready", model.state)
 * }
 * ```
 *
 * The dispatcher is restored when the call returns, so run it on the JVM and native targets, where
 * the returned [TestResult] has already completed.
 *
 * @param dispatcher The dispatcher installed as `Dispatchers.Main` and used to run the test.
 * @param timeout The real-time limit for the body, or `null` for `runTest`'s default.
 * @param testBody The test, run in a [TestScope] that uses [dispatcher].
 * @return The `runTest` result.
 */
public fun runTestWithMain(
    dispatcher: TestDispatcher = StandardTestDispatcher(),
    timeout: Duration? = null,
    testBody: suspend TestScope.() -> Unit,
): TestResult {
    val fixture = MainDispatcherFixture(dispatcher)
    fixture.install()
    try {
        return if (timeout == null) {
            runTest(dispatcher, testBody = testBody)
        } else {
            runTest(dispatcher, timeout, testBody)
        }
    } finally {
        fixture.reset()
    }
}
