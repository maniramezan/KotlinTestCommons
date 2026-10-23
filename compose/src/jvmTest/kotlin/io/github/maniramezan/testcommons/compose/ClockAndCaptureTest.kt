package io.github.maniramezan.testcommons.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ClockAndCaptureTest {
    @Test
    fun advancesFrameByFrame() =
        runComposeUiTest {
            var frames = 0
            mainClock.autoAdvance = false
            setContent {
                LaunchedEffect(Unit) {
                    repeat(10) { withFrameNanos { frames++ } }
                }
            }
            mainClock.advanceFrames(0)
            assertFalse(mainClock.autoAdvance)
            val before = frames
            mainClock.advanceFrames(3)
            assertEquals(before + 3, frames)
            assertFailsWith<IllegalArgumentException> { mainClock.advanceFrames(-1) }
        }

    @Test
    fun capturesPngBytes() =
        runComposeUiTest {
            setContent { Box(Modifier.size(10.dp).background(Color.Red).testTag("box")) }
            val png = onNodeWithTag("box").captureToPng()
            assertContentEquals(
                byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()),
                png.copyOf(4),
            )
        }
}
