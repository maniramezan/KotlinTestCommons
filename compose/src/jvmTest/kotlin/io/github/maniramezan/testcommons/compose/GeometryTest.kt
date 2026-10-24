package io.github.maniramezan.testcommons.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.maniramezan.testcommons.Frame
import io.github.maniramezan.testcommons.ReadingDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeometryTest {
    @Test
    fun rejectsEmptyBoundsInEitherArgument() = runComposeUiTest {
        setContent {
            Row {
                Box(Modifier.size(0.dp).testTag("empty"))
                Box(Modifier.size(0.dp, 40.dp).testTag("zeroWidth"))
                Box(Modifier.size(40.dp, 0.dp).testTag("zeroHeight"))
                Box(Modifier.size(40.dp).testTag("visible"))
            }
        }
        val visible = onNodeWithTag("visible")
        for (tag in listOf("empty", "zeroWidth", "zeroHeight")) {
            val empty = onNodeWithTag(tag)
            val firstFailure = assertFailsWith<AssertionError> { assertNoOverlap(empty, visible) }
            val secondFailure = assertFailsWith<AssertionError> { assertNoOverlap(visible, empty) }
            assertTrue(firstFailure.message.orEmpty().contains("usable"))
            assertTrue(secondFailure.message.orEmpty().contains("usable"))
        }
    }

    @Test
    fun rejectsBoundsClippedAwayByTheRoot() = runComposeUiTest {
        setContent {
            Box(Modifier.size(100.dp).clipToBounds()) {
                Box(Modifier.offset(10_000.dp, 0.dp).size(40.dp).testTag("outside"))
                Box(Modifier.size(40.dp).testTag("visible"))
            }
        }
        val outside = onNodeWithTag("outside")
        assertEquals(0f, outside.frameInRoot().width)
        assertFailsWith<AssertionError> { assertNoOverlap(outside, onNodeWithTag("visible")) }
    }

    @Test
    fun checksReadingOrderOverlapAndContainment() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    Box(Modifier.size(200.dp).testTag("container")) {
                        Row {
                            Box(Modifier.size(40.dp).testTag("first"))
                            Box(Modifier.size(40.dp).testTag("second"))
                        }
                        Box(Modifier.offset(20.dp, 0.dp).size(40.dp).testTag("overlapping"))
                        Box(Modifier.offset(190.dp, 0.dp).size(40.dp).testTag("spilling"))
                    }
                }
            }
            val first = onNodeWithTag("first")
            val second = onNodeWithTag("second")
            assertEquals(Frame(160f, 0f, 200f, 40f), first.frameInRoot())
            assertReadingOrder(first, second, ReadingDirection.RightToLeft)
            assertFailsWith<AssertionError> { assertReadingOrder(first, second, ReadingDirection.LeftToRight) }
            assertNoOverlap(first, second)
            assertFailsWith<AssertionError> { assertNoOverlap(second, onNodeWithTag("overlapping")) }
            first.assertContainedIn(onNodeWithTag("container"))
            assertFailsWith<AssertionError> { onNodeWithTag("spilling").assertContainedIn(onNodeWithTag("container")) }
        }
}
