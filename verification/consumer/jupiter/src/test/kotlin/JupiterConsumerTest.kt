package io.github.maniramezan.testcommons.verification

import io.github.maniramezan.testcommons.junit.MainDispatcherExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals

class JupiterConsumerTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun usesTheConsumersJupiterRuntime() = runTest(mainDispatcher.dispatcher) {
        assertEquals("Main", withContext(Dispatchers.Main) { "Main" })
    }
}
