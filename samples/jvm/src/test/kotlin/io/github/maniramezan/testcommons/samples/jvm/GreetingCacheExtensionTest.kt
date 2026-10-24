package io.github.maniramezan.testcommons.samples.jvm

import io.github.maniramezan.testcommons.CallRecorder
import io.github.maniramezan.testcommons.junit.MainDispatcherExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals

class GreetingCacheExtensionTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun recordsCallsWithTheJupiterExtension() = runTest(mainDispatcher.dispatcher) {
        val requests = CallRecorder<String>()
        val cache = GreetingCache(
            fetch = { name ->
                requests.record(name)
                "Hello, $name!"
            },
            nowMillis = { 0 },
        )
        withContext(Dispatchers.Main) {
            assertEquals("Hello, Ada!", cache.greeting("Ada"))
            assertEquals("Hello, Ada!", cache.greeting("Ada"))
        }
        assertEquals(listOf("Ada"), requests.values())
    }
}
