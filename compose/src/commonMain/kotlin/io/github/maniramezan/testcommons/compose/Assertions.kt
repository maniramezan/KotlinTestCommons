package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The minimum touch target from the Material accessibility guidelines. */
public val MinimumTouchTargetSize: Dp = 48.dp

/**
 * Asserts the node is at least [minSize] wide and tall.
 *
 * @param minSize The minimum width and height.
 * @return This interaction, for chaining.
 */
public fun SemanticsNodeInteraction.assertMinimumTouchTarget(
    minSize: Dp = MinimumTouchTargetSize,
): SemanticsNodeInteraction = assertWidthIsAtLeast(minSize).assertHeightIsAtLeast(minSize)

/**
 * Replaces the node's text and asserts the field now holds exactly [text].
 *
 * Catches fields that reformat or reject input, which a plain `performTextReplacement` hides.
 * Password fields report their obscured text, so verify those through app state instead.
 *
 * @param text The new contents.
 * @return This interaction, for chaining.
 * @throws AssertionError when the field's text differs after replacement.
 */
public fun SemanticsNodeInteraction.replaceTextVerified(text: String): SemanticsNodeInteraction {
    performTextReplacement(text)
    val actual = fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.EditableText) { null }?.text
    if (actual != text) throw AssertionError("Expected the field to contain '$text' but it contains '$actual'")
    return this
}
