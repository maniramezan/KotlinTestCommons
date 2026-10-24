package io.github.maniramezan.testcommons

import kotlin.math.abs

/**
 * Reading-order checks from frame geometry alone, shared by Compose tests, instrumentation
 * tests, and device drivers.
 *
 * Rows are compared by vertical center with an exclusive tolerance. Horizontal order compares
 * centers and does not require frames to be disjoint. Unusable frames and nonpositive or
 * nonfinite tolerances never match.
 */
public object ReadingOrder {
    /** The default row tolerance: one unit of the frames' coordinate space. */
    public const val DEFAULT_ROW_TOLERANCE: Float = 1f

    /**
     * Returns whether two frames' vertical centers differ by less than [tolerance].
     *
     * @param first One frame.
     * @param second The other frame.
     * @param tolerance A finite, positive, exclusive tolerance.
     * @return `true` when both frames are usable and on the same row.
     */
    public fun sharesRow(first: Frame, second: Frame, tolerance: Float = DEFAULT_ROW_TOLERANCE): Boolean {
        if (!tolerance.isFinite() || tolerance <= 0) return false
        if (!FrameGeometry.isUsable(first) || !FrameGeometry.isUsable(second)) return false
        return abs(first.centerY - second.centerY) < tolerance
    }

    /**
     * Returns whether [second] is read after [first] on the same row.
     *
     * @param second The frame expected later.
     * @param first The frame expected earlier.
     * @param direction The row's reading direction.
     * @param rowTolerance The exclusive tolerance for sharing a row.
     * @return `true` when both frames share a row and [second] comes later in [direction].
     */
    public fun follows(
        second: Frame,
        first: Frame,
        direction: ReadingDirection,
        rowTolerance: Float = DEFAULT_ROW_TOLERANCE,
    ): Boolean {
        if (!sharesRow(first, second, rowTolerance)) return false
        return when (direction) {
            ReadingDirection.LEFT_TO_RIGHT -> second.centerX > first.centerX
            ReadingDirection.RIGHT_TO_LEFT -> second.centerX < first.centerX
        }
    }

    /**
     * Finds the first pair of frames, in ascending key order, whose key and successor share a row.
     *
     * Keys whose successor is missing, does not advance the key, or starts a new row are
     * skipped. Use it to find two adjacent items, such as consecutive days in a calendar
     * row, before checking their order with [follows].
     *
     * @param frames Frames by key, such as item index or date.
     * @param successor Returns the key that should follow a given key.
     * @param rowTolerance The exclusive tolerance for sharing a row.
     * @return The first and second frames, or `null` when no adjacent pair shares a row.
     */
    public fun <K : Comparable<K>> firstAdjacentPair(
        frames: Map<K, Frame>,
        successor: (K) -> K,
        rowTolerance: Float = DEFAULT_ROW_TOLERANCE,
    ): Pair<Frame, Frame>? {
        for (key in frames.keys.sorted()) {
            val next = successor(key)
            if (next <= key) continue
            val first = frames[key] ?: continue
            val second = frames[next] ?: continue
            if (sharesRow(first, second, rowTolerance)) return first to second
        }
        return null
    }
}
