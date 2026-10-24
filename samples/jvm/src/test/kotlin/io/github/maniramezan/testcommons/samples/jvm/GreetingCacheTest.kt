package io.github.maniramezan.testcommons.samples.jvm

import io.github.maniramezan.testcommons.ManualClock
import io.github.maniramezan.testcommons.ScriptedResponder
import io.github.maniramezan.testcommons.TestError
import io.github.maniramezan.testcommons.junit.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlin.time.ExperimentalTime::class)
class GreetingCacheTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun cachesUntilTheExactExpiryBoundary() = runTest(mainDispatcher.dispatcher) {
        val clock = ManualClock()
        val responses = ScriptedResponder<String, String>(listOf(Result.success("first"), Result.success("refreshed")))
        val cache = GreetingCache(responses::respond, clock::currentTimeMillis)
        assertEquals("first", cache.greeting("Ada"))
        clock.advanceBy(999.milliseconds)
        assertEquals("first", cache.greeting("Ada"))
        assertEquals(1, responses.callCount)
        clock.advanceBy(1.milliseconds)
        assertEquals("refreshed", cache.greeting("Ada"))
        assertEquals(listOf("Ada", "Ada"), responses.requests)
    }

    @Test
    fun holdsAnInFlightMainDispatcherRequest() = runTest(mainDispatcher.dispatcher) {
        val responses = ScriptedResponder<String, String>(listOf(Result.success("Hello")))
        val gate = responses.hold(0)
        val cache = GreetingCache(responses::respond, nowMillis = { 0 })
        val request = async(Dispatchers.Main) { cache.greeting("Ada") }
        runCurrent()
        assertEquals(listOf("Ada"), responses.requests)
        assertFalse(request.isCompleted)
        gate.open()
        assertEquals("Hello", request.await())
    }

    @Test
    fun failuresAreNotCachedAndNamesHaveSeparateEntries() = runTest(mainDispatcher.dispatcher) {
        val responses = ScriptedResponder<String, String>(
            listOf(Result.failure(TestError()), Result.success("Ada"), Result.success("Grace")),
        )
        val cache = GreetingCache(responses::respond, nowMillis = { 0 })
        assertFailsWith<TestError> { cache.greeting("Ada") }
        assertEquals("Ada", cache.greeting("Ada"))
        assertEquals("Grace", cache.greeting("Grace"))
        assertEquals("Ada", cache.greeting("Ada"))
        assertEquals(listOf("Ada", "Ada", "Grace"), responses.requests)
    }
}
