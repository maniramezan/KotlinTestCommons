package io.github.maniramezan.testcommons

/**
 * A condition did not match before its deadline.
 *
 * @property lastObservation The most recent value observed before the deadline.
 */
public class ObservationTimeout(public val lastObservation: Any?) :
    Exception("Condition did not match before the deadline; last observation: $lastObservation")
