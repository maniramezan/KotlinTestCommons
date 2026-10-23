package io.github.maniramezan.testcommons.device

import io.github.maniramezan.testcommons.Frame

/**
 * One accessibility node, read once into plain values.
 *
 * Reading every field up front avoids the IPC round trip that each `UiObject2` property access
 * costs, so a whole screen can be queried quickly and consistently. Frames are in screen pixels.
 *
 * @property resourceName The full view ID resource name, such as `com.example:id/save`, or empty.
 * @property text The node's text, or empty.
 * @property contentDescription The content description, or empty.
 * @property hintText The hint or placeholder, or `null`.
 * @property className The widget class, such as `android.widget.Button`.
 * @property packageName The package that owns the window.
 * @property frame The bounds on screen.
 * @property isEnabled Whether the node accepts input.
 * @property isVisibleToUser Whether any part of the node is on screen.
 * @property isClickable Whether the node handles clicks itself.
 * @property isLongClickable Whether the node handles long clicks.
 * @property isPassword Whether the node is a password field, whose text is masked.
 * @property isFocused Whether the node has input focus.
 * @property isSelected Whether the node is selected.
 * @property isChecked Whether the node is checked.
 * @property isScrollable Whether the node scrolls.
 * @property depth The distance from the window root.
 * @property clickableAncestor The nearest clickable ancestor, resolved while walking the tree.
 */
public data class AccessibilityElement(
    val resourceName: String,
    val text: String,
    val contentDescription: String,
    val hintText: String?,
    val className: String,
    val packageName: String,
    val frame: Frame,
    val isEnabled: Boolean,
    val isVisibleToUser: Boolean,
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isPassword: Boolean,
    val isFocused: Boolean,
    val isSelected: Boolean,
    val isChecked: Boolean,
    val isScrollable: Boolean,
    val depth: Int,
    val clickableAncestor: AccessibilityElement?,
) {
    /** The resource entry name, such as `save` for `com.example:id/save`, or empty. */
    val resourceEntryName: String get() = resourceName.substringAfterLast('/')

    /** The text when present, otherwise the content description. */
    val label: String get() = text.ifBlank { contentDescription }

    /**
     * Returns whether this element matches every non-null selector.
     *
     * [id] matches the full resource name, the resource entry name, or the content description
     * (Compose test tags exposed with `testTagsAsResourceId` appear as entry names). [text]
     * matches the text or content description exactly; [containsText] matches either one by
     * substring. With no selectors, every element matches.
     *
     * @param id An ID to match.
     * @param text An exact label to match.
     * @param containsText A label fragment to match.
     * @return `true` when every given selector matches.
     */
    public fun matches(id: String? = null, text: String? = null, containsText: String? = null): Boolean {
        if (id != null && id != resourceName && id != resourceEntryName && id != contentDescription) return false
        if (text != null && text != this.text && text != contentDescription) return false
        if (containsText != null && containsText !in this.text && containsText !in contentDescription) return false
        return true
    }
}
