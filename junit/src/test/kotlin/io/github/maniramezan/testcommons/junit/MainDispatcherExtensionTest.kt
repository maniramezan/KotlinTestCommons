package io.github.maniramezan.testcommons.junit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MainDispatcherExtensionTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun mainRunsOnTheSharedScheduler() =
        runTest(mainDispatcher.dispatcher) {
            var ran = false
            launch(Dispatchers.Main) { ran = true }
            testScheduler.advanceUntilIdle()
            assertTrue(ran)
        }

    companion object {
        @JvmStatic
        @AfterAll
        fun mainIsResetAfterEachTest() {
            assertFailsWith<IllegalStateException> { Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext) }
        }
    }
}
