package io.github.maniramezan.testcommons

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.time.Duration

/**
 * Collects [flow] with both an event budget and a real-time budget.
 *
 * Stops when [until] accepts an event, when [maxCount] events have arrived, when the flow
 * completes, or when [timeout] passes, and reports which one happened. The timeout runs on
 * [Dispatchers.Default], so it measures real time even inside `runTest`; collection stays
 * on the caller's dispatcher. Zero budgets collect nothing.
 *
 * ```kotlin
 * val observation = observeFlow(viewModel.state, maxCount = 5, timeout = 2.seconds) { it.isLoaded }
 * assertEquals(FlowObservation.End.Matched, observation.end)
 * ```
 *
 * @param flow The flow to collect; its failures propagate unchanged.
 * @param maxCount The nonnegative event budget.
 * @param timeout The nonnegative real-time budget.
 * @param until Stops observation when it returns `true`; defaults to collecting up to the limit.
 * @return Collected events and the stopping reason.
 */
public suspend fun <T> observeFlow(
    flow: Flow<T>,
    maxCount: Int,
    timeout: Duration,
    until: (T) -> Boolean = { false },
): FlowObservation<T> {
    require(maxCount >= 0) { "maxCount must be nonnegative" }
    require(!timeout.isNegative()) { "timeout must be nonnegative" }
    currentCoroutineContext().ensureActive()
    if (maxCount == 0) return FlowObservation(emptyList(), FlowObservation.End.LimitReached)
    if (timeout == Duration.ZERO) return FlowObservation(emptyList(), FlowObservation.End.TimedOut)

    val values = mutableListOf<T>()
    val timedOut = AtomicBoolean(false)
    return coroutineScope {
        val collector = async(start = CoroutineStart.UNDISPATCHED) {
            try {
                flow.collect { value ->
                    values += value
                    if (until(value)) throw StopObservation(FlowObservation.End.Matched)
                    if (values.size >= maxCount) throw StopObservation(FlowObservation.End.LimitReached)
                }
                FlowObservation.End.Finished
            } catch (stop: StopObservation) {
                stop.end
            }
        }
        val timer = launch(Dispatchers.Default) {
            delay(timeout)
            timedOut.store(true)
            collector.cancel()
        }
        val end = try {
            collector.await()
        } catch (cancelled: CancellationException) {
            currentCoroutineContext().ensureActive()
            if (!timedOut.load()) throw cancelled
            collector.join()
            FlowObservation.End.TimedOut
        }
        timer.cancel()
        FlowObservation(values.toList(), end)
    }
}

private class StopObservation(val end: FlowObservation.End) : Exception()
