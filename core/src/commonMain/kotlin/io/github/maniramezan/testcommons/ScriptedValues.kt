package io.github.maniramezan.testcommons

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement

/**
 * A deterministic sequence of values, such as dates or IDs, with an explicit exhaustion policy.
 *
 * Safe to share between threads: each value is handed out once, in order.
 *
 * ```kotlin
 * val ids = ScriptedValues(listOf("a", "b"), ScriptedValues.Exhaustion.Fail)
 * val repository = Repository(newId = ids::next)
 * ```
 *
 * @param values Values to hand out in order; an empty script is permitted.
 * @param exhaustion The behavior after the last value; defaults to failing.
 */
public class ScriptedValues<T>(values: List<T>, private val exhaustion: Exhaustion<T> = Exhaustion.Fail) {
    /** Behavior after every supplied value has been consumed. */
    public sealed interface Exhaustion<out T> {
        /** Throw [ScriptedValues.Exhausted]. */
        public data object Fail : Exhaustion<Nothing>

        /** Repeat the last supplied value; an empty script throws [ScriptedValues.Exhausted]. */
        public data object RepeatLast : Exhaustion<Nothing>

        /**
         * Return [value] for every later call.
         *
         * @property value The fallback value.
         */
        public data class Fallback<T>(val value: T) : Exhaustion<T>
    }

    /**
     * Thrown when a script runs out of values and its policy supplies no replacement.
     *
     * Distinct from errors a test scripts or expects, such as [TestError], so an unexpected
     * extra call cannot satisfy an expected failure.
     *
     * @property consumedCount The number of values the script supplied before running out.
     */
    public class Exhausted(public val consumedCount: Int) :
        IllegalStateException("Scripted values exhausted after $consumedCount values")

    private val values = values.toList()
    private val index = AtomicInt(0)

    /** The number of supplied values that have not been consumed. */
    public val remainingCount: Int get() = (values.size - index.load()).coerceAtLeast(0)

    /**
     * Consumes the next value or applies the exhaustion policy.
     *
     * @return The next value, the repeated last value, or the fallback.
     * @throws Exhausted when exhausted without a usable replacement.
     */
    public fun next(): T {
        val position = index.fetchAndIncrement()
        if (position < values.size) return values[position]
        return when (exhaustion) {
            Exhaustion.Fail -> throw Exhausted(values.size)
            Exhaustion.RepeatLast -> if (values.isEmpty()) throw Exhausted(0) else values.last()
            is Exhaustion.Fallback -> exhaustion.value
        }
    }
}
