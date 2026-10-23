package io.github.maniramezan.testcommons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ManualClockTest {
    @Test
    fun reportsTimeOnlyAfterAdvancing() {
        val clock = ManualClock(Instant.fromEpochMilliseconds(1_000))
        val mark = clock.markNow()
        assertEquals(1_000, clock.currentTimeMillis())
        clock.advanceBy(2.seconds)
        assertEquals(3_000, clock.currentTimeMillis())
        assertEquals(Instant.fromEpochMilliseconds(3_000), clock.now())
        assertEquals(2.seconds, mark.elapsedNow())
        assertEquals(2.seconds, clock.markNow() - mark)
        assertEquals(clock.markNow(), mark + 2.seconds)
        assertNotEquals(clock.markNow(), mark)
        clock.advanceTo(1.seconds)
        assertEquals(2.seconds, clock.elapsed)
        assertFailsWith<IllegalArgumentException> { clock.advanceBy(-1.seconds) }
        assertFailsWith<IllegalArgumentException> { mark - ManualClock().markNow() }
    }

    @Test
    fun advancingResumesDueSleepersInDeadlineOrder() =
        runTest {
            val clock = ManualClock()
            val woke = mutableListOf<Int>()
            launch {
                clock.sleep(3.seconds)
                woke += 3
            }
            launch {
                clock.sleep(1.seconds)
                woke += 1
            }
            launch {
                clock.sleepUntil(5.seconds)
                woke += 5
            }
            runCurrent()
            assertEquals(3, clock.sleeperCount)
            clock.advanceBy(3.seconds)
            runCurrent()
            assertEquals(listOf(1, 3), woke)
            assertEquals(1, clock.sleeperCount)
            clock.advanceTo(5.seconds)
            runCurrent()
            assertEquals(listOf(1, 3, 5), woke)
        }

    @Test
    fun pastDeadlinesReturnImmediatelyAndCancellationRemovesTheSleeper() =
        runTest {
            val clock = ManualClock()
            clock.sleep(Duration.ZERO)
            val sleeper = launch { clock.sleep(1.seconds) }
            runCurrent()
            assertEquals(1, clock.sleeperCount)
            sleeper.cancel()
            runCurrent()
            assertEquals(0, clock.sleeperCount)
            assertTrue(sleeper.isCancelled)
        }

    @Test
    fun waitForSleepersObservesRegistrationFromAnotherThread() =
        runTest {
            val clock = ManualClock()
            withContext(Dispatchers.Default) {
                val sleeper = async { clock.sleep(1.seconds) }
                clock.waitForSleepers(1)
                assertFalse(sleeper.isCompleted)
                clock.advanceBy(1.seconds)
                sleeper.await()
            }
            assertFailsWith<IllegalArgumentException> { clock.waitForSleepers(-1) }
        }
}
