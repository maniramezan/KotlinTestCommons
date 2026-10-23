package io.github.maniramezan.testcommons

import kotlin.math.abs

/**
 * Layout checks over [Frame]s in one coordinate space.
 *
 * Every check rejects unusable frames: empty, inverted, or nonfinite ones never match.
 */
public object FrameGeometry {
    /**
     * Returns whether [frame] has finite edges and a positive width and height.
     *
     * @param frame The frame to validate.
     * @return `true` when the frame can take part in layout checks.
     */
    public fun isUsable(frame: Frame): Boolean =
        frame.left.isFinite() &&
            frame.top.isFinite() &&
            frame.right.isFinite() &&
            frame.bottom.isFinite() &&
            frame.width.isFinite() &&
            frame.height.isFinite() &&
            frame.width > 0 &&
            frame.height > 0

    /**
     * Returns whether [child] lies within [container], grown by [tolerance] on every side.
     *
     * @param child The inner frame.
     * @param container The outer frame.
     * @param tolerance A finite, nonnegative allowance for rounding.
     * @return `true` when both frames are usable and the child fits.
     */
    public fun contains(child: Frame, container: Frame, tolerance: Float = 0f): Boolean {
        if (!isUsable(child) || !isUsable(container) || !tolerance.isFinite() || tolerance < 0) return false
        val expanded = Frame(
            container.left - tolerance,
            container.top - tolerance,
            container.right + tolerance,
            container.bottom + tolerance,
        )
        return isUsable(expanded) &&
            child.left >= expanded.left &&
            child.top >= expanded.top &&
            child.right <= expanded.right &&
            child.bottom <= expanded.bottom
    }

    /**
     * Returns whether two frames share a region of positive area; touching edges do not overlap.
     *
     * @param first One frame.
     * @param second The other frame.
     * @return `true` when both frames are usable and intersect.
     */
    public fun overlaps(first: Frame, second: Frame): Boolean =
        isUsable(first) &&
            isUsable(second) &&
            maxOf(first.left, second.left) < minOf(first.right, second.right) &&
            maxOf(first.top, second.top) < minOf(first.bottom, second.bottom)

    /**
     * Returns whether two frames' horizontal centers differ by less than [tolerance].
     *
     * @param first One frame.
     * @param second The other frame.
     * @param tolerance A finite, positive, exclusive tolerance.
     * @return `true` when both frames are usable and centered on the same vertical line.
     */
    public fun horizontallyAligned(first: Frame, second: Frame, tolerance: Float = 1f): Boolean {
        if (!isUsable(first) || !isUsable(second) || !tolerance.isFinite() || tolerance <= 0) return false
        return abs(first.centerX - second.centerX) < tolerance
    }

    /**
     * Returns the horizontal distance between two frames, or zero when they overlap horizontally.
     *
     * @param first One frame.
     * @param second The other frame.
     * @return The gap, or `null` when either frame is unusable.
     */
    public fun horizontalGap(first: Frame, second: Frame): Float? {
        if (!isUsable(first) || !isUsable(second)) return null
        return maxOf(0f, maxOf(first.left, second.left) - minOf(first.right, second.right))
    }
}
