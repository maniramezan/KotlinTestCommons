package io.github.maniramezan.testcommons.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaitsTest {
    @Test
    fun waitsForContentThatAppearsLater() =
        runComposeUiTest {
            setContent {
                var loaded by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(500)
                    loaded = true
                }
                Column {
                    if (loaded) {
                        BasicText("Ready", Modifier.testTag("ready"))
                        Box(Modifier.size(24.dp).semantics { contentDescription = "Close" })
                    } else {
                        BasicText("Loading")
                    }
                }
            }
            waitForText("Ready").assertIsDisplayed()
            waitForTag("ready").assertIsDisplayed()
            waitForTag("ready", useUnmergedTree = true).assertIsDisplayed()
            waitForText("Rea", substring = true)
            waitForContentDescription("Close")
            assertEquals("ready", waitForAnyTag("missing", "ready"))
            waitUntilGone(hasText("Loading"))
        }

    @Test
    fun offScreenNodesAreNotDisplayed() =
        runComposeUiTest {
            setContent {
                BasicText("Hidden", Modifier.offset(x = 10_000.dp).testTag("hidden"))
            }
            assertFailsWith<ComposeTimeoutException> { waitUntilDisplayed(hasText("Hidden"), timeoutMillis = 100) }
            waitUntilGone(hasText("Hidden"), timeoutMillis = 100)
            assertFailsWith<ComposeTimeoutException> { waitForTag("hidden", timeoutMillis = 100) }
            assertFailsWith<ComposeTimeoutException> { waitForAnyTag("absent", timeoutMillis = 100) }
            assertFailsWith<IllegalArgumentException> { waitForAnyTag() }
        }

    @Test
    fun returnsTheFirstDisplayedMatchAmongDuplicates() =
        runComposeUiTest {
            setContent {
                Column {
                    BasicText("Item", Modifier.offset(x = 10_000.dp).testTag("offscreen"))
                    BasicText("Item", Modifier.testTag("visible"))
                }
            }
            waitUntilDisplayed(hasText("Item")).assertIsDisplayed()
            waitUntilDisplayed(hasTestTag("visible"), useUnmergedTree = true)
        }
}
