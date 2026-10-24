package io.github.maniramezan.testcommons

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ObserveFlowTest {
    @Test
    fun stopsWhenThePredicateMatches() =
        runTest {
            val observation = observeFlow(flowOf(1, 2, 3, 4), maxCount = 10, timeout = 1.seconds) { it == 2 }
            assertEquals(FlowObservation(listOf(1, 2), FlowObservation.End.Matched), observation)
        }

    @Test
    fun stopsAtTheLimitOrWhenTheFlowFinishes() =
        runTest {
            assertEquals(
                FlowObservation(listOf(1, 2), FlowObservation.End.LimitReached),
                observeFlow(flowOf(1, 2, 3), maxCount = 2, timeout = 1.seconds),
            )
            assertEquals(
                FlowObservation(listOf(1), FlowObservation.End.Finished),
                observeFlow(flowOf(1), maxCount = 2, timeout = 1.seconds),
            )
        }

    @Test
    fun timesOutInRealTimeWithTheEventsSoFar() =
        runTest {
            val observation = observeFlow(
                flow {
                    emit(1)
                    awaitCancellation()
                },
                maxCount = 5,
                timeout = 20.milliseconds,
            )
            assertEquals(FlowObservation(listOf(1), FlowObservation.End.TimedOut), observation)
        }

    @Test
    fun timeoutIncludesSynchronousUpstreamWork() =
        runTest {
            val observation = observeFlow(
                flow {
                    emit(1)
                    val started = TimeSource.Monotonic.markNow()
                    while (started.elapsedNow() < 200.milliseconds) {
                        continue
                    }
                    emit(2)
                },
                maxCount = 5,
                timeout = 20.milliseconds,
            )
            assertEquals(FlowObservation(listOf(1), FlowObservation.End.TimedOut), observation)
        }

    @Test
    fun zeroBudgetsCollectNothing() =
        runTest {
            assertEquals(FlowObservation.End.LimitReached, observeFlow(flowOf(1), 0, 1.seconds).end)
            assertEquals(FlowObservation.End.TimedOut, observeFlow(flowOf(1), 1, Duration.ZERO).end)
            assertFailsWith<IllegalArgumentException> { observeFlow(flowOf(1), -1, 1.seconds) }
            assertFailsWith<IllegalArgumentException> { observeFlow(flowOf(1), 1, -1.seconds) }
        }

    @Test
    fun flowFailuresPropagate() =
        runTest {
            assertFailsWith<TestError> { observeFlow(flow<Int> { throw TestError() }, 1, 1.seconds) }
        }

    @Test
    fun callerCancellationPropagates() =
        runTest {
            val events = MutableSharedFlow<Int>()
            val observation = async { observeFlow(events, 5, 10.seconds) }
            runCurrent()
            observation.cancel()
            assertFailsWith<CancellationException> { observation.await() }
            assertTrue(observation.isCancelled)
        }
}
