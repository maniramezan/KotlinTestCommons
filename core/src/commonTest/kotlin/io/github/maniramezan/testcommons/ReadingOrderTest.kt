package io.github.maniramezan.testcommons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingOrderTest {
    private val left = Frame.fromSize(0f, 0f, 10f, 10f)
    private val right = Frame.fromSize(20f, 0f, 10f, 10f)
    private val below = Frame.fromSize(20f, 30f, 10f, 10f)

    @Test
    fun aFrameToTheRightFollowsOnlyLeftToRight() {
        assertTrue(ReadingOrder.follows(right, left, ReadingDirection.LEFT_TO_RIGHT))
        assertFalse(ReadingOrder.follows(right, left, ReadingDirection.RIGHT_TO_LEFT))
    }

    @Test
    fun aFrameToTheLeftFollowsOnlyRightToLeft() {
        assertTrue(ReadingOrder.follows(left, right, ReadingDirection.RIGHT_TO_LEFT))
        assertFalse(ReadingOrder.follows(left, right, ReadingDirection.LEFT_TO_RIGHT))
    }

    @Test
    fun framesOnDifferentRowsNeverFollowEachOther() {
        assertFalse(ReadingOrder.follows(below, left, ReadingDirection.LEFT_TO_RIGHT))
        assertFalse(ReadingOrder.follows(below, left, ReadingDirection.RIGHT_TO_LEFT))
    }

    @Test
    fun rowsCompareCentersWithAnExclusiveTolerance() {
        val taller = Frame.fromSize(20f, -5f, 10f, 20f)
        val sameTop = Frame.fromSize(20f, 0f, 10f, 20f)
        val shifted = Frame.fromSize(20f, 1f, 10f, 10f)
        val nudged = Frame.fromSize(20f, 3f, 10f, 10f)
        assertTrue(ReadingOrder.sharesRow(left, taller))
        assertTrue(ReadingOrder.follows(taller, left, ReadingDirection.LEFT_TO_RIGHT))
        assertFalse(ReadingOrder.sharesRow(left, sameTop))
        assertFalse(ReadingOrder.sharesRow(left, shifted))
        assertTrue(ReadingOrder.sharesRow(left, shifted, tolerance = 1.01f))
        assertTrue(ReadingOrder.follows(shifted, left, ReadingDirection.LEFT_TO_RIGHT, rowTolerance = 1.01f))
        assertFalse(ReadingOrder.sharesRow(left, nudged))
        assertTrue(ReadingOrder.sharesRow(left, nudged, tolerance = 5f))
    }

    @Test
    fun invalidTolerancesNeverMatch() {
        for (tolerance in listOf(0f, -1f, Float.POSITIVE_INFINITY, Float.NaN)) {
            assertFalse(ReadingOrder.sharesRow(left, right, tolerance))
            assertFalse(ReadingOrder.follows(right, left, ReadingDirection.LEFT_TO_RIGHT, tolerance))
        }
    }

    @Test
    fun unusableFramesNeverMatch() {
        val frames = listOf(
            Frame(0f, 0f, 0f, 0f),
            Frame(Float.NaN, 0f, 10f, 10f),
            Frame(0f, 0f, Float.POSITIVE_INFINITY, 10f),
        )
        for (frame in frames) {
            assertFalse(ReadingOrder.sharesRow(left, frame))
            assertFalse(ReadingOrder.sharesRow(frame, left))
            assertFalse(ReadingOrder.follows(frame, left, ReadingDirection.LEFT_TO_RIGHT))
            assertFalse(ReadingOrder.follows(left, frame, ReadingDirection.RIGHT_TO_LEFT))
        }
    }

    @Test
    fun aFrameDoesNotFollowItselfOrAFrameWithTheSameCenter() {
        val narrower = Frame.fromSize(2f, 0f, 6f, 10f)
        for (direction in ReadingDirection.entries) {
            assertFalse(ReadingOrder.follows(narrower, left, direction))
            assertFalse(ReadingOrder.follows(left, left, direction))
        }
    }

    @Test
    fun firstAdjacentPairSkipsRowBreaks() {
        val frames = mapOf(
            1 to Frame.fromSize(40f, 0f, 10f, 10f),
            2 to Frame.fromSize(0f, 20f, 10f, 10f),
            3 to Frame.fromSize(20f, 20f, 10f, 10f),
        )
        assertEquals(frames.getValue(2) to frames.getValue(3), ReadingOrder.firstAdjacentPair(frames, { it + 1 }))
    }

    @Test
    fun firstAdjacentPairIsNullWithoutAdjacentNeighbours() {
        val wrapped = mapOf(1 to left, 2 to below)
        assertNull(ReadingOrder.firstAdjacentPair(wrapped, { it + 1 }))
        assertNull(ReadingOrder.firstAdjacentPair(emptyMap<Int, Frame>(), { it + 1 }))
        assertNull(ReadingOrder.firstAdjacentPair(mapOf(1 to left), { it + 1 }))
        assertNull(ReadingOrder.firstAdjacentPair(mapOf("a" to left, "b" to right), { "a" }))
    }

    @Test
    fun firstAdjacentPairSkipsMissingSuccessors() {
        val frames = mapOf("a" to left, "c" to below, "d" to below, "e" to right)
        assertEquals(below to below, ReadingOrder.firstAdjacentPair(frames, { (it[0] + 1).toString() }))
    }
}
