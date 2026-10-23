package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithContentDescription

/**
 * Matches nodes whose test tag starts with [prefix], such as every keyed row built from one
 * automation identifier.
 *
 * @param prefix The tag prefix.
 * @return The matcher.
 */
public fun hasTestTagStartingWith(prefix: String): SemanticsMatcher =
    SemanticsMatcher("TestTag starts with '$prefix'") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

/**
 * Finds the node whose content description is exactly [label], case-sensitively.
 *
 * Use it for icon buttons and images, where a partial match would hide a missing label.
 *
 * @param label The exact content description.
 * @param useUnmergedTree Whether to search the unmerged semantics tree.
 * @return The node interaction.
 */
public fun SemanticsNodeInteractionsProvider.onNodeWithRequiredContentDescription(
    label: String,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction =
    onNodeWithContentDescription(label, substring = false, ignoreCase = false, useUnmergedTree = useUnmergedTree)
