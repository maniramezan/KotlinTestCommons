package io.github.maniramezan.testcommons.device

import io.github.maniramezan.testcommons.Frame

internal fun element(
    text: String = "",
    contentDescription: String = "",
    resourceName: String = "",
    isClickable: Boolean = false,
    depth: Int = 0,
    clickableAncestor: AccessibilityElement? = null,
) = AccessibilityElement(
    resourceName = resourceName,
    text = text,
    contentDescription = contentDescription,
    hintText = null,
    className = "android.view.View",
    packageName = "com.example",
    frame = Frame(0f, 0f, 10f, 10f),
    isEnabled = true,
    isVisibleToUser = true,
    isClickable = isClickable,
    isLongClickable = false,
    isPassword = false,
    isFocused = false,
    isSelected = false,
    isChecked = false,
    isScrollable = false,
    depth = depth,
    clickableAncestor = clickableAncestor,
)
