package io.github.maniramezan.testcommons.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

private val LocalThemeName = staticCompositionLocalOf { "none" }

class RuleWaitsTest {
    @get:Rule
    val composeRule = ThemedComposeRule(theme = { content ->
        CompositionLocalProvider(LocalThemeName provides "brand", content = content)
    })

    @Test
    fun themedContentAndRuleWaits() {
        composeRule.setContent {
            Column {
                BasicText("Theme: ${LocalThemeName.current}", Modifier.testTag("title"))
                Box(Modifier.size(24.dp).semantics { contentDescription = "Close" })
            }
        }
        composeRule.waitForText("Theme: brand")
        composeRule.waitForTag("title").assertIsDisplayed()
        composeRule.waitForTag("title", useUnmergedTree = true).assertIsDisplayed()
        composeRule.waitForText("brand", substring = true)
        composeRule.waitForContentDescription("Close")
        composeRule.waitUntilDisplayed(hasText("Theme: brand"))
        composeRule.waitUntilGone(hasText("Loading"))
        assertEquals("title", composeRule.waitForAnyTag("title"))
    }
}
