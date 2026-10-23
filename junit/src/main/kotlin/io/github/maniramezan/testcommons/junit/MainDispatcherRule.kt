package io.github.maniramezan.testcommons.junit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit 4 rule that replaces [Dispatchers.Main] with [dispatcher] for each test and always
 * restores it afterward, including when the test fails.
 *
 * Pass `rule.dispatcher` to `runTest` so the code under test and the test share one scheduler:
 *
 * ```kotlin
 * @get:Rule val mainDispatcher = MainDispatcherRule()
 *
 * @Test fun loads() = runTest(mainDispatcher.dispatcher) { ... }
 * ```
 *
 * @property dispatcher The dispatcher installed as `Dispatchers.Main`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class MainDispatcherRule(public val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
