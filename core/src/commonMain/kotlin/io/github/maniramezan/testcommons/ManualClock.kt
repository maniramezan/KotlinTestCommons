package io.github.maniramezan.testcommons

import kotlinx.coroutines.CompletableDeferred
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.concurrent.atomics.update
import kotlin.time.Clock
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * A clock whose time moves only when a test calls [advanceBy] or [advanceTo].
 *
 * Inject it wherever code reads the time or sleeps through an abstraction you own, to make
 * debouncing, retries, and expiry deterministic. It is a [Clock] (wall time starting at
 * [start]), a [TimeSource.WithComparableMarks] (monotonic marks), and a millisecond source
 * through [currentTimeMillis], for code that takes `clockMs: () -> Long`.
 *
 * Advancing resumes every sleeper whose deadline has been reached, in deadline order.
 * Start the code under test, wait for it to suspend with [waitForSleepers], then advance:
 * advancing before a sleep registers does not wake it later. Each advance moves time
 * directly to its target, so a coroutine that wakes and sleeps again measures its next
 * deadline from the new time.
 *
 * For code that uses `delay` directly, prefer `runTest` and its virtual time instead.
 *
 * @param start The wall-clock instant reported before any advance.
 */
public class ManualClock(public val start: Instant = Instant.fromEpochMilliseconds(0)) :
    Clock,
    TimeSource.WithComparableMarks {
    private class Sleeper(val deadline: Duration, val signal: CompletableDeferred<Unit>)

    private class State(val elapsed: Duration, val sleepers: Map<Long, Sleeper>)

    private val state = AtomicReference(State(Duration.ZERO, emptyMap()))
    private val nextSleeperId = AtomicLong(0)

    /** The manual time elapsed since [start]. */
    public val elapsed: Duration get() = state.load().elapsed

    /** The number of coroutines currently suspended in [sleep] or [sleepUntil]. */
    public val sleeperCount: Int get() = state.load().sleepers.size

    /** Returns [start] plus the manual time elapsed so far. */
    override fun now(): Instant = start + elapsed

    /** Returns the current wall-clock time in epoch milliseconds. */
    public fun currentTimeMillis(): Long = now().toEpochMilliseconds()

    /** Returns a mark at the current manual time. */
    override fun markNow(): ComparableTimeMark = ManualTimeMark(this, elapsed)

    /**
     * Suspends until [duration] of manual time has passed.
     *
     * @param duration The manual time to wait; zero or negative returns immediately.
     */
    public suspend fun sleep(duration: Duration) {
        sleepUntil(elapsed + duration)
    }

    /**
     * Suspends until [elapsed] reaches [deadline].
     *
     * Deadlines at or before the current time return immediately. Cancelling the
     * calling coroutine removes only this sleeper.
     *
     * @param deadline The elapsed time since [start] at which to resume.
     */
    public suspend fun sleepUntil(deadline: Duration) {
        val id = nextSleeperId.fetchAndIncrement()
        val sleeper = Sleeper(deadline, CompletableDeferred())
        var registered = false
        state.update { current ->
            registered = deadline > current.elapsed
            if (registered) State(current.elapsed, current.sleepers + (id to sleeper)) else current
        }
        if (!registered) return
        try {
            sleeper.signal.await()
        } finally {
            state.update { State(it.elapsed, it.sleepers - id) }
        }
    }

    /**
     * Advances manual time and resumes every sleeper whose deadline has been reached.
     *
     * @param duration A nonnegative amount of manual time.
     */
    public fun advanceBy(duration: Duration) {
        require(!duration.isNegative()) { "ManualClock cannot move backward" }
        advance { it + duration }
    }

    /**
     * Moves manual time to [elapsed] and resumes sleepers due at or before it.
     *
     * A target earlier than the current time leaves time unchanged.
     *
     * @param elapsed The target time since [start].
     */
    public fun advanceTo(elapsed: Duration) {
        advance { maxOf(it, elapsed) }
    }

    /**
     * Waits in real time until at least [count] coroutines are sleeping on this clock.
     *
     * @param count The nonnegative number of sleepers to wait for.
     * @param timeout The real-time observation budget.
     * @throws ObservationTimeout with the last sleeper count when the deadline passes.
     */
    public suspend fun waitForSleepers(count: Int, timeout: Duration = 2.seconds) {
        require(count >= 0) { "count must be nonnegative" }
        waitUntil(timeout, observe = { sleeperCount }) { it >= count }
    }

    private fun advance(update: (Duration) -> Duration) {
        var due = emptyList<Sleeper>()
        state.update { current ->
            val now = update(current.elapsed)
            val (ready, waiting) = current.sleepers.entries.partition { it.value.deadline <= now }
            due = ready.map { it.value }.sortedBy { it.deadline }
            State(now, waiting.associate { it.key to it.value })
        }
        due.forEach { it.signal.complete(Unit) }
    }

    private class ManualTimeMark(private val clock: ManualClock, private val at: Duration) : ComparableTimeMark {
        override fun elapsedNow(): Duration = clock.elapsed - at

        override fun plus(duration: Duration): ComparableTimeMark = ManualTimeMark(clock, at + duration)

        override fun minus(other: ComparableTimeMark): Duration {
            require(other is ManualTimeMark && other.clock === clock) {
                "Marks from different time sources cannot be compared"
            }
            return at - other.at
        }

        override fun equals(other: Any?): Boolean = other is ManualTimeMark && other.clock === clock && other.at == at

        override fun hashCode(): Int = at.hashCode()
    }
}
