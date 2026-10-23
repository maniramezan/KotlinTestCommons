package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.test.MainTestClock

/**
 * Stops automatic advancing and steps the clock by [count] frames, so a test can observe
 * an animation or effect frame by frame.
 *
 * @param count The nonnegative number of frames to advance.
 */
public fun MainTestClock.advanceFrames(count: Int) {
    require(count >= 0) { "count must be nonnegative" }
    autoAdvance = false
    repeat(count) { advanceTimeByFrame() }
}
