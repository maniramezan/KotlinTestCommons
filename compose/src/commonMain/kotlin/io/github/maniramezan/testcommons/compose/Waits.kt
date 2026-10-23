package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed

/** The default wait budget for UI helpers, in milliseconds. */
public const val DEFAULT_UI_TIMEOUT_MILLIS: Long = 5_000L

/** A longer wait budget for network-backed screens or cold starts, in milliseconds. */
public const val LONG_UI_TIMEOUT_MILLIS: Long = 15_000L

/**
 * Waits until a node matching [matcher] is displayed and returns it.
 *
 * Unlike `waitUntilAtLeastOneExists`, this requires the node to be visible on screen, not just
 * present in the semantics tree. When several nodes match, the first displayed one is returned.
 *
 * @param matcher Selects the node.
 * @param timeoutMillis The wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The displayed node.
 * @throws androidx.compose.ui.test.ComposeTimeoutException when no node is displayed in time.
 */
public fun ComposeUiTest.waitUntilDisplayed(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = awaitDisplayed(this, { d, t, c ->
    waitUntil(d, t, c)
}, matcher, timeoutMillis, useUnmergedTree)

/**
 * Waits until no node matching [matcher] is displayed; nodes that exist off screen count as gone.
 *
 * @param matcher Selects the node.
 * @param timeoutMillis The wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @throws androidx.compose.ui.test.ComposeTimeoutException when a node is still displayed.
 */
public fun ComposeUiTest.waitUntilGone(
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
public fun ComposeUiTest.waitForText(
    text: String,
    substring: Boolean = false,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
): SemanticsNodeInteraction = waitUntilDisplayed(hasText(text, substring = substring), timeoutMillis)

/**
 * Waits until a node with content description [description] is displayed and returns it.
 *
 * @param description The exact content description.
 * @param timeoutMillis The wait budget.
 * @return The displayed node.
 */
public fun ComposeUiTest.waitForContentDescription(
    description: String,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
): SemanticsNodeInteraction = waitUntilDisplayed(hasContentDescription(description), timeoutMillis)

/**
 * Waits until a node with any of [tags] exists in the unmerged tree, and returns the first tag
 * found in argument order. Use it when a screen can land in one of several states.
 *
 * @param tags The candidate test tags.
 * @param timeoutMillis The wait budget.
 * @return The tag that appeared.
 */
public fun ComposeUiTest.waitForAnyTag(
    vararg tags: String,
    timeoutMillis: Long = DEFAULT_UI_TIMEOUT_MILLIS,
): String = awaitAnyTag(this, { d, t, c -> waitUntil(d, t, c) }, tags.toList(), timeoutMillis)

internal typealias WaitUntil = (String, Long, () -> Boolean) -> Unit

internal fun awaitDisplayed(
    provider: SemanticsNodeInteractionsProvider,
    waitUntil: WaitUntil,
    matcher: SemanticsMatcher,
    timeoutMillis: Long,
    useUnmergedTree: Boolean,
): SemanticsNodeInteraction {
    var displayed: SemanticsNodeInteraction? = null
    waitUntil("${matcher.description} is displayed", timeoutMillis) {
        displayed = provider.firstDisplayed(matcher, useUnmergedTree)
        displayed != null
    }
    return checkNotNull(displayed)
}

internal fun awaitGone(
    provider: SemanticsNodeInteractionsProvider,
    waitUntil: WaitUntil,
    matcher: SemanticsMatcher,
    timeoutMillis: Long,
    useUnmergedTree: Boolean,
) {
    waitUntil("${matcher.description} is gone", timeoutMillis) {
        provider.firstDisplayed(matcher, useUnmergedTree) == null
    }
}

internal fun awaitAnyTag(
    provider: SemanticsNodeInteractionsProvider,
    waitUntil: WaitUntil,
    tags: List<String>,
    timeoutMillis: Long,
): String {
    require(tags.isNotEmpty()) { "Pass at least one tag" }
    var found: String? = null
    waitUntil("any of $tags exists", timeoutMillis) {
        found = tags.firstOrNull { tag ->
            provider.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        found != null
    }
    return checkNotNull(found)
}

private fun SemanticsNodeInteractionsProvider.firstDisplayed(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean,
): SemanticsNodeInteraction? {
    val nodes = onAllNodes(matcher, useUnmergedTree)
    val count = nodes.fetchSemanticsNodes(atLeastOneRootRequired = false).size
    return (0 until count).map { nodes[it] }.firstOrNull { it.isDisplayed() }
}
