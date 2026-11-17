package io.github.maniramezan.testcommons.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScrollingTest {
    @Test
    fun scrollsTheLargestContainerOnTheRequestedAxis() =
        runComposeUiTest {
            setContent {
                LazyColumn(Modifier.fillMaxSize().testTag("feed")) {
                    item {
                        LazyRow(Modifier.height(50.dp).testTag("carousel")) {
                            items(50) { BasicText("Card $it", Modifier.size(100.dp)) }
                        }
                    }
                    items(100) { BasicText("Row $it", Modifier.height(50.dp)) }
                }
            }
            assertEquals(onNodeWithTag("feed").fetchSemanticsNode().id, scrollContainer().fetchSemanticsNode().id)
            assertEquals(
                onNodeWithTag("carousel").fetchSemanticsNode().id,
                scrollContainer(vertical = false).fetchSemanticsNode().id,
            )
            onNodeWithText("Row 0").assertIsDisplayed()
            scrollByViewport()
            waitUntilGone(hasText("Row 0"))
            scrollByViewport(forward = false)
            waitUntilDisplayed(hasText("Row 0"))
            scrollByViewport(vertical = false)
            assertFailsWith<IllegalArgumentException> { scrollByViewport(fraction = 0f) }
            scrollToNode(hasText("Row 80")).assertIsDisplayed()
            scrollToNode(hasText("Row 79")).assertIsDisplayed()
            scrollToNode(hasText("Row 2"), container = onNodeWithTag("feed")).assertIsDisplayed()
        }

    @Test
    fun failsWithoutAScrollableNode() =
        runComposeUiTest {
            setContent { Column { Box(Modifier.size(10.dp)) } }
            assertFailsWith<IllegalStateException> { scrollContainer() }
        }
}
