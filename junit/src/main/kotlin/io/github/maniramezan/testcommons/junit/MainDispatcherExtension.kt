package io.github.maniramezan.testcommons.junit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit 5 extension that replaces [Dispatchers.Main] with [dispatcher] for each test and always
 * restores it afterward.
 *
 * Register it as a field so the test can share its dispatcher with `runTest`:
 *
 * ```kotlin
 * @JvmField @RegisterExtension val mainDispatcher = MainDispatcherExtension()
 *
 * @Test fun loads() = runTest(mainDispatcher.dispatcher) { ... }
 * ```
 *
 * This artifact declares JUnit Jupiter as `compileOnly`; projects using the extension already
 * depend on it.
 *
 * @property dispatcher The dispatcher installed as `Dispatchers.Main`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class MainDispatcherExtension(public val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    BeforeEachCallback,
    AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
