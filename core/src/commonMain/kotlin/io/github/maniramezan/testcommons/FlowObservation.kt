package io.github.maniramezan.testcommons

/**
 * Events collected by [observeFlow] and the reason it stopped.
 *
 * @property values Events in delivery order, including a matching event when present.
 * @property end The reason observation stopped.
 */
public data class FlowObservation<T>(val values: List<T>, val end: End) {
    /** Why observation stopped. */
    public enum class End {
        /** The predicate accepted an event. */
        Matched,

        /** The event budget was consumed. */
        LimitReached,

        /** The flow completed before matching or reaching the limit. */
        Finished,

        /** The real-time deadline passed. */
        TimedOut,
    }
}
