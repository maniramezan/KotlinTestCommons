package io.github.maniramezan.testcommons.device

/**
 * Returns the element to tap for the single element matching [predicate].
 *
 * Prefers the match itself when it is clickable, then its nearest clickable ancestor, and
 * falls back to the match. Compose often splits a button into sibling semantics nodes where
 * the one carrying the text is not the clickable one, so tapping the raw match can miss.
 *
 * @param elements A snapshot from [AccessibilitySnapshot.capture].
 * @param predicate Selects the element, for example `{ it.matches(text = "Save") }`.
 * @return The element to tap, or `null` when no element or more than one element matches.
 */
public fun findTapTarget(
    elements: List<AccessibilityElement>,
    predicate: (AccessibilityElement) -> Boolean,
): AccessibilityElement? {
    val match = elements.filter(predicate).singleOrNull() ?: return null
    return when {
        match.isClickable -> match
        else -> match.clickableAncestor ?: match
    }
}
