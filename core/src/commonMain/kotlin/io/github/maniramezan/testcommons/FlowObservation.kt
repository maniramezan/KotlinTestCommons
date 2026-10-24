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
        MATCHED,

        /** The event budget was consumed. */
        LIMIT_REACHED,

        /** The flow completed before matching or reaching the limit. */
        FINISHED,

        /** The real-time deadline passed. */
        TIMED_OUT,
    }
}
