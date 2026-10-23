package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction

/**
 * Returns the largest scrollable node on the given axis.
 *
 * Choosing by axis keeps a nested horizontal carousel from absorbing a vertical scroll, and
 * choosing by area picks the screen's main list. Scrolling through the returned node uses
 * semantics actions, which work even when a lazy list reports zero-size bounds.
 *
 * @param vertical Whether to look for a vertical (`true`) or horizontal scroll container.
 * @return The container.
 * @throws IllegalStateException when the screen has no scrollable node on that axis.
 */
public fun SemanticsNodeInteractionsProvider.scrollContainer(vertical: Boolean = true): SemanticsNodeInteraction {
    val axis = if (vertical) {
        SemanticsProperties.VerticalScrollAxisRange
    } else {
        SemanticsProperties.HorizontalScrollAxisRange
    }
    val target = onAllNodes(SemanticsMatcher.keyIsDefined(axis), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { SemanticsActions.ScrollBy in it.config }
        .maxByOrNull { it.boundsInRoot.width * it.boundsInRoot.height }
        ?: error("No scrollable node on the current screen for the ${if (vertical) "vertical" else "horizontal"} axis")
    return onNode(SemanticsMatcher("scroll container id=${target.id}") { it.id == target.id }, useUnmergedTree = true)
}

/**
 * Scrolls the main container by [fraction] of the root's size on the given axis.
 *
 * @param vertical Whether to scroll vertically.
 * @param forward Whether to scroll toward the end (down or right in left-to-right layouts).
 * @param fraction The share of the viewport to scroll, from 0 (exclusive) to 1.
 */
public fun SemanticsNodeInteractionsProvider.scrollByViewport(
    vertical: Boolean = true,
    forward: Boolean = true,
    fraction: Float = 0.8f,
) {
    require(fraction > 0f && fraction <= 1f) { "fraction must be in (0, 1]" }
    val viewport = onRoot().fetchSemanticsNode().boundsInRoot
    val extent = if (vertical) viewport.height else viewport.width
    check(extent > 0f) { "The root has no size to scroll by" }
    val delta = extent * fraction * if (forward) 1f else -1f
    scrollContainer(vertical).performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
        if (vertical) scrollBy(0f, delta) else scrollBy(delta, 0f)
    }
}

/**
 * Scrolls a container until a node matching [target] is composed, and returns the target.
 *
 * Without [container], the innermost scrollable node already holding the target is used; when
 * the target is not composed yet, as with a far-away lazy-list item, the largest vertical
 * container from [scrollContainer] is searched instead.
 *
 * @param target Selects the node to reveal.
 * @param container The container to scroll, or `null` to choose one.
 * @param useUnmergedTree Whether to search the unmerged semantics tree for the target.
 * @return The revealed node.
 */
public fun SemanticsNodeInteractionsProvider.scrollToNode(
    target: SemanticsMatcher,
    container: SemanticsNodeInteraction? = null,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction {
    val scroller = container ?: run {
        val holder = onAllNodes(hasScrollToNodeAction() and hasAnyDescendant(target), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .minByOrNull { it.boundsInRoot.width * it.boundsInRoot.height }
        if (holder == null) {
            scrollContainer(vertical = true)
        } else {
            onNode(SemanticsMatcher("scroll container id=${holder.id}") { it.id == holder.id }, useUnmergedTree = true)
        }
    }
    scroller.performScrollToNode(target)
    return onNode(target, useUnmergedTree)
}
