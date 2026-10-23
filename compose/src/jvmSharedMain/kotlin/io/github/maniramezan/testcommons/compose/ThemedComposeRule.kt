package io.github.maniramezan.testcommons.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule

/**
 * A compose rule that wraps every `setContent` in the app's theme, replacing per-test
 * `setContent { AppTheme { ... } }` boilerplate.
 *
 * ```kotlin
 * @get:Rule val composeRule = ThemedComposeRule(theme = { content -> AppTheme(content = content) })
 *
 * @Test fun showsTitle() {
 *     composeRule.setContent { Title("Ready") }
 *     composeRule.waitForText("Ready")
 * }
 * ```
 *
 * @param theme Wraps test content, for example in the app theme or a dark-mode variant.
 * @param delegate The rule to wrap; defaults to `createComposeRule()`.
 */
public class ThemedComposeRule(
    private val theme: @Composable (content: @Composable () -> Unit) -> Unit,
    private val delegate: ComposeContentTestRule = createComposeRule(),
) : ComposeContentTestRule by delegate {
    override fun setContent(composable: @Composable () -> Unit) {
        delegate.setContent { theme(composable) }
    }
}
