package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Waits until a node matching [matcher] is displayed and returns it.
 *
 * The JUnit 4 rule counterpart of [androidx.compose.ui.test.ComposeUiTest.waitUntilDisplayed].
 *
 * @param matcher Selects the node.
 * @param timeoutMillis The wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The displayed node.
 */
public fun ComposeTestRule.waitUntilDisplayed(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = awaitDisplayed(this, { d, t, c ->
    waitUntil(d, t, c)
}, matcher, timeoutMillis, useUnmergedTree)

/**
 * Waits until no node matching [matcher] is displayed.
 *
 * @param matcher Selects the node.
 * @param timeoutMillis The wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 */
public fun ComposeTestRule.waitUntilGone(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
    useUnmergedTree: Boolean = false,
) {
    awaitGone(this, { d, t, c -> waitUntil(d, t, c) }, matcher, timeoutMillis, useUnmergedTree)
}

/**
 * Waits until a node showing [text] is displayed and returns it.
 *
 * @param text The text to find.
 * @param substring Whether a partial match is enough.
 * @param timeoutMillis The wait budget.
 * @return The displayed node.
 */
public fun ComposeTestRule.waitForText(
    text: String,
    substring: Boolean = false,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
): SemanticsNodeInteraction = waitUntilDisplayed(hasText(text, substring = substring), timeoutMillis)

/**
 * Waits until a node with [tag] is displayed and returns it.
 *
 * @param tag The exact test tag.
 * @param timeoutMillis The wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The first displayed node with the tag.
 * @throws androidx.compose.ui.test.ComposeTimeoutException when no matching node is displayed in time.
 */
public fun ComposeTestRule.waitForTag(
    tag: String,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = waitUntilDisplayed(hasTestTag(tag), timeoutMillis, useUnmergedTree)

/**
 * Waits until a node with content description [description] is displayed and returns it.
 *
 * @param description The exact content description.
 * @param timeoutMillis The wait budget.
 * @return The displayed node.
 */
public fun ComposeTestRule.waitForContentDescription(
    description: String,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
): SemanticsNodeInteraction = waitUntilDisplayed(hasContentDescription(description), timeoutMillis)

/**
 * Waits until a node with any of [tags] exists, and returns the first tag found in argument order.
 *
 * @param tags The candidate test tags.
 * @param timeoutMillis The wait budget.
 * @return The tag that appeared.
 */
public fun ComposeTestRule.waitForAnyTag(
    vararg tags: String,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
): String = awaitAnyTag(this, { d, t, c -> waitUntil(d, t, c) }, tags.toList(), timeoutMillis)
