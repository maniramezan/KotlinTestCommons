package io.github.maniramezan.testcommons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WaitUntilTest {
    @Test
    fun returnsTheFirstMatchingObservation() =
        runTest {
            var reads = 0
            val value = withContext(Dispatchers.Default) {
                waitUntil(1.seconds, pollInterval = 1.milliseconds, observe = { ++reads }) { it >= 3 }
            }
            assertEquals(3, value)
        }

    @Test
    fun observesOnceForAZeroTimeoutAndReportsTheLastValue() =
        runTest {
            var reads = 0
            val timeout = assertFailsWith<ObservationTimeout> {
                waitUntil(Duration.ZERO, observe = { ++reads }) { false }
            }
            assertEquals(1, timeout.lastObservation)
            assertEquals(1, reads)
        }

    @Test
    fun propagatesObservationErrorsAndRejectsInvalidBudgets() =
        runTest {
            assertFailsWith<TestError> { waitUntil(1.seconds, observe = { throw TestError() }) { true } }
            assertFailsWith<IllegalArgumentException> { waitUntil(-1.seconds, observe = { 0 }) { true } }
            assertFailsWith<IllegalArgumentException> {
                waitUntil(1.seconds, pollInterval = Duration.ZERO, observe = { 0 }) { true }
            }
        }
}
