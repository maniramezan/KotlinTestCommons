package io.github.maniramezan.testcommons.device

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import io.github.maniramezan.testcommons.Frame

/**
 * Captures every window's accessibility tree as [AccessibilityElement]s in one pass.
 *
 * Use it to query a whole screen quickly, including Compose UI, other apps, and system dialogs,
 * then act on the result with UiAutomator. The returned list is in depth-first order.
 */
public object AccessibilitySnapshot {
    /** The default maximum tree depth; deeper nodes are skipped. */
    public const val DEFAULT_MAX_DEPTH: Int = 60

    /**
     * Reads the current accessibility trees.
     *
     * Clears the accessibility cache first on API 34+, so a navigation is never answered with the
     * previous screen. Enables interactive-window retrieval so windows of apps other than the
     * instrumentation target are included. That setting stays on for the rest of the test process.
     *
     * @param packageName When set, keeps only elements from this package.
     * @param maxDepth The maximum depth to walk from each window root.
     * @param uiAutomation The automation connection to read from.
     * @return The elements, in depth-first order.
     */
    public fun capture(
        packageName: String? = null,
        maxDepth: Int = DEFAULT_MAX_DEPTH,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): List<AccessibilityElement> {
        enableInteractiveWindows(uiAutomation)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching { uiAutomation.clearCache() }
        }
        val windows = runCatching { uiAutomation.windows }.getOrNull().orEmpty()
        val roots = windows.mapNotNull { it.root }.ifEmpty { listOfNotNull(uiAutomation.rootInActiveWindow) }
        val elements = ArrayList<AccessibilityElement>()
        try {
            for (root in roots) collect(AccessibilityNodeSource(root), maxDepth, elements)
        } finally {
            @Suppress("DEPRECATION")
            for (root in roots) runCatching { root.recycle() }
            @Suppress("DEPRECATION")
            for (window in windows) runCatching { window.recycle() }
        }
        return if (packageName.isNullOrBlank()) elements else elements.filter { it.packageName == packageName }
    }
}

/**
 * Turns on interactive-window retrieval so [UiAutomation.getWindows] includes windows of apps
 * other than the instrumentation target.
 *
 * This changes the automation connection's service info for the rest of the test process.
 */
internal fun enableInteractiveWindows(uiAutomation: UiAutomation) {
    runCatching {
        val info = uiAutomation.serviceInfo ?: return
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            uiAutomation.serviceInfo = info
        }
    }
}

/** A node in a tree being walked; lets the walk be tested without a device. */
internal interface NodeSource {
    val childCount: Int

    fun read(depth: Int, clickableAncestor: AccessibilityElement?): AccessibilityElement

    fun child(index: Int): NodeSource?

    fun release()
}

/**
 * Appends [root] and its descendants to [into] depth-first, skipping nodes deeper than [maxDepth]
 * and nodes that go stale while being read.
 */
internal fun collect(
    root: NodeSource,
    maxDepth: Int,
    into: MutableList<AccessibilityElement>,
    depth: Int = 0,
    clickableAncestor: AccessibilityElement? = null,
) {
    if (depth > maxDepth) return
    val element = runCatching { root.read(depth, clickableAncestor) }.getOrNull() ?: return
    into += element
    val ancestorForChildren = if (element.isClickable) element else clickableAncestor
    for (index in 0 until root.childCount) {
        val child = runCatching { root.child(index) }.getOrNull() ?: continue
        try {
            collect(child, maxDepth, into, depth + 1, ancestorForChildren)
        } finally {
            child.release()
        }
    }
}

private class AccessibilityNodeSource(private val node: AccessibilityNodeInfo) : NodeSource {
    override val childCount: Int get() = node.childCount

    // `isChecked` is deprecated on API 36 in favor of `checked`, which older devices lack.
    @Suppress("DEPRECATION")
    override fun read(depth: Int, clickableAncestor: AccessibilityElement?): AccessibilityElement {
        val bounds = Rect().also(node::getBoundsInScreen)
        return AccessibilityElement(
            resourceName = node.viewIdResourceName.orEmpty(),
            text = node.text?.toString().orEmpty(),
            contentDescription = node.contentDescription?.toString().orEmpty(),
            hintText = node.hintText?.toString()?.ifBlank { null },
            className = node.className?.toString().orEmpty(),
            packageName = node.packageName?.toString().orEmpty(),
            frame = Frame(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat()),
            isEnabled = node.isEnabled,
            isVisibleToUser = node.isVisibleToUser,
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isPassword = node.isPassword,
            isFocused = node.isFocused,
            isSelected = node.isSelected,
            isChecked = node.isChecked,
            isScrollable = node.isScrollable,
            depth = depth,
            clickableAncestor = clickableAncestor,
        )
    }

    override fun child(index: Int): NodeSource? = node.getChild(index)?.let(::AccessibilityNodeSource)

    @Suppress("DEPRECATION")
    override fun release() {
        runCatching { node.recycle() }
    }
}
