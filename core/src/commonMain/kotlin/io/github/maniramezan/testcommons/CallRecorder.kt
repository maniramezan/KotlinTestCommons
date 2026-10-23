package io.github.maniramezan.testcommons

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Records values, such as arguments passed to a faked dependency, in the order calls arrive.
 *
 * Recording never suspends, so it is safe from synchronous callbacks and from any thread.
 * Finish the operation under test before inspecting values; calls racing on different
 * threads are ordered by whichever records first.
 *
 * ```kotlin
 * val recorder = CallRecorder<String>()
 * val client = FakeClient(onRequest = recorder::record)
 * ```
 */
public class CallRecorder<T> {
    private val recorded = AtomicReference<List<T>>(emptyList())

    /** The number of recorded calls. */
    public val count: Int get() = recorded.load().size

    /**
     * Appends [value] to the call history.
     *
     * @param value The argument or event to record.
     */
    public fun record(value: T) {
        recorded.update { it + value }
    }

    /**
     * Returns a snapshot of every recorded value in arrival order.
     *
     * Later calls to [record] do not change a previously returned list.
     *
     * @return The call history, or an empty list before the first call.
     */
    public fun values(): List<T> = recorded.load()

    /**
     * Returns the most recently recorded value.
     *
     * @return The last value, or `null` when nothing has been recorded.
     */
    public fun lastValue(): T? = recorded.load().lastOrNull()

    /** Clears the history; pending [waitForCalls] calls observe the new history. */
    public fun reset() {
        recorded.store(emptyList())
    }

    /**
     * Waits in real time until at least [count] calls have been recorded.
     *
     * @param count The nonnegative minimum call count.
     * @param timeout The total monotonic wait budget.
     * @return A snapshot containing at least [count] calls.
     * @throws ObservationTimeout with the last history when the deadline passes.
     */
    public suspend fun waitForCalls(count: Int, timeout: Duration = 2.seconds): List<T> {
        require(count >= 0) { "count must be nonnegative" }
        return waitUntil(timeout, pollInterval = 1.milliseconds, observe = ::values) { it.size >= count }
    }
}
