package io.github.maniramezan.testcommons

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Observes state until it matches [matching], polling every [pollInterval].
 *
 * The deadline is measured with the monotonic clock in real time. Inside `runTest`,
 * `delay` uses virtual time, so call this from `withContext(Dispatchers.Default)` when the
 * observed work runs on another thread; prefer `advanceUntilIdle()` for work on the test
 * scheduler. The [observe] block must return promptly and cooperate with cancellation:
 * a deadline cannot interrupt arbitrary code. One observation is made even for a zero
 * timeout, and errors from [observe] propagate unchanged.
 *
 * ```kotlin
 * val count = waitUntil(2.seconds, observe = { model.count }) { it >= 3 }
 * ```
 *
 * @param timeout The total observation budget, at least zero.
 * @param pollInterval The positive delay between observations.
 * @param observe Reads the state without modifying it.
 * @param matching Returns whether an observation satisfies the condition.
 * @return The first matching observation.
 * @throws ObservationTimeout with the last observation when the deadline passes.
 */
public suspend fun <T> waitUntil(
    timeout: Duration,
    pollInterval: Duration = 10.milliseconds,
    observe: suspend () -> T,
    matching: (T) -> Boolean,
): T {
    require(!timeout.isNegative()) { "timeout must be nonnegative" }
    require(pollInterval.isPositive()) { "pollInterval must be positive" }
    val deadline = TimeSource.Monotonic.markNow() + timeout
    while (true) {
        currentCoroutineContext().ensureActive()
        val value = observe()
        currentCoroutineContext().ensureActive()
        if (matching(value)) return value
        val remaining = -deadline.elapsedNow()
        if (!remaining.isPositive()) throw ObservationTimeout(value)
        delay(minOf(pollInterval, remaining))
    }
}
