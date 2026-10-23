package io.github.maniramezan.testcommons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CallRecorderTest {
    @Test
    fun recordsValuesInArrivalOrder() {
        val recorder = CallRecorder<String>()
        assertNull(recorder.lastValue())
        recorder.record("a")
        val snapshot = recorder.values()
        recorder.record("b")
        assertEquals(listOf("a"), snapshot)
        assertEquals(listOf("a", "b"), recorder.values())
        assertEquals("b", recorder.lastValue())
        assertEquals(2, recorder.count)
        recorder.reset()
        assertEquals(0, recorder.count)
    }

    @Test
    fun recordsEveryConcurrentCall() =
        runTest {
            val recorder = CallRecorder<Int>()
            withContext(Dispatchers.Default) {
                List(8) { worker -> launch { repeat(250) { recorder.record(worker) } } }
            }
            assertEquals(2000, recorder.count)
        }

    @Test
    fun waitsForCallsFromAnotherThread() =
        runTest {
            val recorder = CallRecorder<Int>()
            val calls = withContext(Dispatchers.Default) {
                val waiter = async { recorder.waitForCalls(2) }
                launch {
                    delay(10.milliseconds)
                    recorder.record(1)
                    recorder.record(2)
                }
                waiter.await()
            }
            assertEquals(listOf(1, 2), calls)
        }

    @Test
    fun waitForCallsTimesOutWithTheLastHistory() =
        runTest {
            val recorder = CallRecorder<Int>()
            recorder.record(1)
            val timeout = withContext(Dispatchers.Default) {
                assertFailsWith<ObservationTimeout> { recorder.waitForCalls(2, timeout = 20.milliseconds) }
            }
            assertEquals(listOf(1), timeout.lastObservation)
            assertFailsWith<IllegalArgumentException> { recorder.waitForCalls(-1, 1.seconds) }
        }
}
