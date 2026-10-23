package io.github.maniramezan.testcommons

/**
 * An axis-aligned rectangle in one coordinate space, such as pixels in a window.
 *
 * Adapters in the `compose` and `device` artifacts build frames from Compose bounds and
 * accessibility nodes. A frame is usable by [FrameGeometry] and [ReadingOrder] only when
 * every edge is finite and it has a positive width and height.
 *
 * @property left The minimum x coordinate.
 * @property top The minimum y coordinate.
 * @property right The maximum x coordinate.
 * @property bottom The maximum y coordinate.
 */
public data class Frame(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    /** The horizontal size, `right - left`. */
    val width: Float get() = right - left

    /** The vertical size, `bottom - top`. */
    val height: Float get() = bottom - top

    /** The horizontal center. */
    val centerX: Float get() = left + width / 2

    /** The vertical center. */
    val centerY: Float get() = top + height / 2

    public companion object {
        /**
         * Creates a frame from its origin and size.
         *
         * @param x The left edge.
         * @param y The top edge.
         * @param width The horizontal size.
         * @param height The vertical size.
         * @return The frame spanning `x..x + width` and `y..y + height`.
         */
        public fun fromSize(x: Float, y: Float, width: Float, height: Float): Frame =
            Frame(x, y, x + width, y + height)
    }
}
