package io.github.maniramezan.testcommons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrameGeometryTest {
    private val first = Frame.fromSize(0f, 0f, 10f, 10f)
    private val touching = Frame.fromSize(10f, 0f, 10f, 10f)
    private val empty = Frame(0f, 0f, 0f, 0f)

    private fun Frame.offset(dx: Float, dy: Float) = Frame(left + dx, top + dy, right + dx, bottom + dy)

    @Test
    fun usableFramesNeedFiniteEdgesAndPositiveSize() {
        assertTrue(FrameGeometry.isUsable(first))
        assertFalse(FrameGeometry.isUsable(empty))
        assertFalse(FrameGeometry.isUsable(Frame(10f, 0f, 0f, 10f)))
        assertFalse(FrameGeometry.isUsable(Frame(0f, 0f, Float.POSITIVE_INFINITY, 10f)))
        assertFalse(FrameGeometry.isUsable(Frame(Float.NaN, 0f, 10f, 10f)))
        assertFalse(FrameGeometry.isUsable(Frame(-Float.MAX_VALUE, 0f, Float.MAX_VALUE, 10f)))
    }

    @Test
    fun overlapRejectsTouchingEdgesAndEmptyFrames() {
        assertFalse(FrameGeometry.overlaps(first, touching))
        assertTrue(FrameGeometry.overlaps(first, first.offset(9f, 0f)))
        assertFalse(FrameGeometry.overlaps(first, first.offset(0f, 10f)))
        assertFalse(FrameGeometry.overlaps(empty, first))
    }

    @Test
    fun containmentHonoursTolerance() {
        assertTrue(FrameGeometry.contains(first, first))
        assertFalse(FrameGeometry.contains(empty, first))
        assertFalse(FrameGeometry.contains(first.offset(1f, 0f), first))
        assertTrue(FrameGeometry.contains(first.offset(1f, 0f), first, tolerance = 1f))
        assertFalse(FrameGeometry.contains(first, first, tolerance = Float.NaN))
        assertFalse(FrameGeometry.contains(first, first, tolerance = -1f))
        assertFalse(FrameGeometry.contains(first, first, tolerance = Float.MAX_VALUE))
    }

    @Test
    fun horizontalAlignmentUsesAnExclusiveTolerance() {
        assertTrue(FrameGeometry.horizontallyAligned(first, first.offset(0f, 10f)))
        assertFalse(FrameGeometry.horizontallyAligned(first, touching))
        assertFalse(FrameGeometry.horizontallyAligned(first, first.offset(1f, 0f)))
        assertFalse(FrameGeometry.horizontallyAligned(empty, first))
        assertFalse(FrameGeometry.horizontallyAligned(first, first, tolerance = Float.NaN))
        assertFalse(FrameGeometry.horizontallyAligned(first, first, tolerance = 0f))
    }

    @Test
    fun horizontalGapIsZeroForOverlapAndNullForUnusableFrames() {
        assertEquals(5f, FrameGeometry.horizontalGap(first, first.offset(15f, 0f)))
        assertEquals(5f, FrameGeometry.horizontalGap(first.offset(15f, 0f), first))
        assertEquals(0f, FrameGeometry.horizontalGap(touching, first))
        assertEquals(0f, FrameGeometry.horizontalGap(first, first.offset(9f, 0f)))
        assertNull(FrameGeometry.horizontalGap(empty, first))
    }

    @Test
    fun frameDerivesSizeAndCenter() {
        val frame = Frame.fromSize(2f, 4f, 6f, 8f)
        assertEquals(Frame(2f, 4f, 8f, 12f), frame)
        assertEquals(6f, frame.width)
        assertEquals(8f, frame.height)
        assertEquals(5f, frame.centerX)
        assertEquals(8f, frame.centerY)
    }
}
