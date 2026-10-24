package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The default wait budget for UI helpers. */
public val DefaultUiTimeout: Duration = 5.seconds

/** A longer wait budget for network-backed screens or cold starts. */
public val LongUiTimeout: Duration = 15.seconds

/**
 * Waits until a node matching [matcher] is displayed and returns it.
 *
 * Unlike `waitUntilAtLeastOneExists`, this requires the node to be visible on screen, not just
 * present in the semantics tree. When several nodes match, the first displayed one is returned.
 *
 * @param matcher Selects the node.
 * @param timeout The nonnegative wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The displayed node.
 * @throws androidx.compose.ui.test.ComposeTimeoutException when no node is displayed in time.
 */
public fun ComposeUiTest.waitUntilDisplayed(
    matcher: SemanticsMatcher,
    timeout: Duration = DefaultUiTimeout,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = awaitDisplayed(this, { d, t, c ->
    waitUntil(d, t, c)
}, matcher, timeout, useUnmergedTree)

/**
 * Waits until no node matching [matcher] is displayed; nodes that exist off screen count as gone.
 *
 * @param matcher Selects the node.
 * @param timeout The nonnegative wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @throws androidx.compose.ui.test.ComposeTimeoutException when a node is still displayed.
 */
public fun ComposeUiTest.waitUntilGone(
    matcher: SemanticsMatcher,
    timeout: Duration = DefaultUiTimeout,
    useUnmergedTree: Boolean = false,
) {
    awaitGone(this, { d, t, c -> waitUntil(d, t, c) }, matcher, timeout, useUnmergedTree)
}

/**
 * Waits until a node showing [text] is displayed and returns it.
 *
 * @param text The text to find.
 * @param substring Whether a partial match is enough.
 * @param timeout The nonnegative wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The displayed node.
 */
public fun ComposeUiTest.waitForText(
    text: String,
    substring: Boolean = false,
    timeout: Duration = DefaultUiTimeout,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = waitUntilDisplayed(hasText(text, substring = substring), timeout, useUnmergedTree)

/**
 * Waits until a node with [tag] is displayed and returns it.
 *
 * @param tag The exact test tag.
 * @param timeout The nonnegative wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The first displayed node with the tag.
 * @throws androidx.compose.ui.test.ComposeTimeoutException when no matching node is displayed in time.
 */
public fun ComposeUiTest.waitForTag(
    tag: String,
    timeout: Duration = DefaultUiTimeout,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = waitUntilDisplayed(hasTestTag(tag), timeout, useUnmergedTree)

/**
 * Waits until a node with content description [description] is displayed and returns it.
 *
 * @param description The exact content description.
 * @param timeout The nonnegative wait budget.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The displayed node.
 */
public fun ComposeUiTest.waitForContentDescription(
    description: String,
    timeout: Duration = DefaultUiTimeout,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = waitUntilDisplayed(hasContentDescription(description), timeout, useUnmergedTree)

/**
 * Waits until a node with any of [tags] exists in the unmerged tree, and returns the first tag
 * found in argument order. Use it when a screen can land in one of several states.
 *
 * @param tags The candidate test tags.
 * @param timeout The nonnegative wait budget.
 * @return The tag that appeared.
 */
public fun ComposeUiTest.waitForAnyTag(
    vararg tags: String,
    timeout: Duration = DefaultUiTimeout,
): String = awaitAnyTag(this, { d, t, c -> waitUntil(d, t, c) }, tags.toList(), timeout)

internal typealias WaitUntil = (String, Long, () -> Boolean) -> Unit

internal fun awaitDisplayed(
    provider: SemanticsNodeInteractionsProvider,
    waitUntil: WaitUntil,
    matcher: SemanticsMatcher,
    timeout: Duration,
    useUnmergedTree: Boolean,
): SemanticsNodeInteraction {
    var displayed: SemanticsNodeInteraction? = null
    waitUntil("${matcher.description} is displayed", waitMillis(timeout)) {
        displayed = provider.firstDisplayed(matcher, useUnmergedTree)
        displayed != null
    }
    return checkNotNull(displayed)
}

internal fun awaitGone(
    provider: SemanticsNodeInteractionsProvider,
    waitUntil: WaitUntil,
    matcher: SemanticsMatcher,
    timeout: Duration,
    useUnmergedTree: Boolean,
) {
    waitUntil("${matcher.description} is gone", waitMillis(timeout)) {
        provider.firstDisplayed(matcher, useUnmergedTree) == null
    }
}

internal fun awaitAnyTag(
    provider: SemanticsNodeInteractionsProvider,
    waitUntil: WaitUntil,
    tags: List<String>,
    timeout: Duration,
): String {
    require(tags.isNotEmpty()) { "Pass at least one tag" }
    var found: String? = null
    waitUntil("any of $tags exists", waitMillis(timeout)) {
        found = tags.firstOrNull { tag ->
            provider.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        found != null
    }
    return checkNotNull(found)
}

internal fun waitMillis(timeout: Duration): Long {
    require(!timeout.isNegative()) { "timeout must be nonnegative" }
    return timeout.inWholeMilliseconds
}

private fun SemanticsNodeInteractionsProvider.firstDisplayed(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean,
): SemanticsNodeInteraction? {
    val nodes = onAllNodes(matcher, useUnmergedTree)
    val count = nodes.fetchSemanticsNodes(atLeastOneRootRequired = false).size
    return (0 until count).map { nodes[it] }.firstOrNull { it.isDisplayed() }
}
