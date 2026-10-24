package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.test.SemanticsNodeInteraction
import io.github.maniramezan.testcommons.Frame
import io.github.maniramezan.testcommons.FrameGeometry
import io.github.maniramezan.testcommons.ReadingDirection
import io.github.maniramezan.testcommons.ReadingOrder

/**
 * Returns the node's bounds in the root's pixel coordinates as a [Frame].
 *
 * @return The frame, usable with `FrameGeometry` and `ReadingOrder`.
 */
public fun SemanticsNodeInteraction.frameInRoot(): Frame {
    val bounds = fetchSemanticsNode().boundsInRoot
    return Frame(bounds.left, bounds.top, bounds.right, bounds.bottom)
}

/**
 * Asserts that [second] is read after [first] on the same row in [direction].
 *
 * @param first The node expected earlier.
 * @param second The node expected later.
 * @param direction The row's reading direction.
 * @param rowTolerance The exclusive vertical-center tolerance, in pixels.
 * @throws AssertionError with both frames when the order does not hold.
 */
public fun assertReadingOrder(
    first: SemanticsNodeInteraction,
    second: SemanticsNodeInteraction,
    direction: ReadingDirection,
    rowTolerance: Float = ReadingOrder.DEFAULT_ROW_TOLERANCE,
) {
    val firstFrame = first.frameInRoot()
    val secondFrame = second.frameInRoot()
    if (!ReadingOrder.follows(secondFrame, firstFrame, direction, rowTolerance)) {
        throw AssertionError("Expected $secondFrame to follow $firstFrame on one row reading $direction")
    }
}

/**
 * Asserts that two nodes have usable bounds and do not overlap; touching edges are allowed.
 *
 * Empty, clipped-away, and nonfinite bounds fail rather than passing because they have no intersection.
 *
 * @param first One node.
 * @param second The other node.
 * @throws AssertionError with both frames when either is unusable or they overlap.
 */
public fun assertNoOverlap(first: SemanticsNodeInteraction, second: SemanticsNodeInteraction) {
    val firstFrame = first.frameInRoot()
    val secondFrame = second.frameInRoot()
    if (!FrameGeometry.isUsable(firstFrame) || !FrameGeometry.isUsable(secondFrame) ||
        FrameGeometry.overlaps(firstFrame, secondFrame)
    ) {
        throw AssertionError("Expected usable, non-overlapping frames but found $firstFrame and $secondFrame")
    }
}

/**
 * Asserts that this node lies within [container], allowing [tolerance] pixels for rounding.
 *
 * @param container The enclosing node.
 * @param tolerance A nonnegative allowance on every side, in pixels.
 * @return This interaction, for chaining.
 * @throws AssertionError with both frames when the node is outside the container.
 */
public fun SemanticsNodeInteraction.assertContainedIn(
    container: SemanticsNodeInteraction,
    tolerance: Float = 0f,
): SemanticsNodeInteraction {
    val child = frameInRoot()
    val outer = container.frameInRoot()
    if (!FrameGeometry.contains(child, outer, tolerance)) {
        throw AssertionError("Expected $child to be within $outer")
    }
    return this
}
