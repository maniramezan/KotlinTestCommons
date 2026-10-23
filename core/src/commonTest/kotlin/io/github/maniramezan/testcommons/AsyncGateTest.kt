package io.github.maniramezan.testcommons

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncGateTest {
    @Test
    fun openResumesEveryWaiterAndStaysOpen() =
        runTest {
            val gate = AsyncGate()
            var resumed = 0
            repeat(3) {
                launch {
                    gate.await()
                    resumed++
                }
            }
            runCurrent()
            assertEquals(3, gate.waiterCount)
            assertFalse(gate.isOpen)
            gate.open()
            runCurrent()
            assertEquals(3, resumed)
            assertEquals(0, gate.waiterCount)
            assertTrue(gate.isOpen)
            gate.open()
            gate.await()
        }

    @Test
    fun cancellingOneWaiterLeavesTheOthers() =
        runTest {
            val gate = AsyncGate()
            val cancelled = launch { gate.await() }
            var resumed = false
            launch {
                gate.await()
                resumed = true
            }
            runCurrent()
            cancelled.cancel()
            runCurrent()
            assertEquals(1, gate.waiterCount)
            gate.open()
            runCurrent()
            assertTrue(resumed)
            assertTrue(cancelled.isCancelled)
        }
}
