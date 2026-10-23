package io.github.maniramezan.testcommons.device

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CollectTest {
    private class FakeNode(
        val text: String,
        val clickable: Boolean = false,
        val children: List<FakeNode?> = emptyList(),
        val stale: Boolean = false,
    ) : NodeSource {
        var released = false

        override val childCount: Int get() = children.size

        override fun read(depth: Int, clickableAncestor: AccessibilityElement?): AccessibilityElement {
            check(!stale) { "stale" }
            return element(text = text, isClickable = clickable, depth = depth, clickableAncestor = clickableAncestor)
        }

        override fun child(index: Int): NodeSource? = children[index] ?: throw IllegalStateException("gone")

        override fun release() {
            released = true
        }
    }

    @Test
    fun walksDepthFirstAndResolvesClickableAncestors() {
        val label = FakeNode("Label")
        val button = FakeNode("Button", clickable = true, children = listOf(label))
        val root = FakeNode("Root", children = listOf(button, FakeNode("Sibling")))
        val elements = mutableListOf<AccessibilityElement>()
        collect(root, maxDepth = 10, into = elements)
        assertEquals(listOf("Root", "Button", "Label", "Sibling"), elements.map { it.text })
        assertEquals(listOf(0, 1, 2, 1), elements.map { it.depth })
        assertSame(elements[1], elements[2].clickableAncestor)
        assertNull(elements[3].clickableAncestor)
        assertTrue(label.released)
    }

    @Test
    fun skipsDeepStaleAndMissingNodes() {
        val deep = FakeNode("Deep")
        val root = FakeNode(
            "Root",
            children = listOf(FakeNode("Child", children = listOf(deep)), FakeNode("Stale", stale = true), null),
        )
        val elements = mutableListOf<AccessibilityElement>()
        collect(root, maxDepth = 1, into = elements)
        assertEquals(listOf("Root", "Child"), elements.map { it.text })
    }

    @Test
    fun findTapTargetPrefersClickableMatchesThenAncestors() {
        val button = element(text = "Container", isClickable = true)
        val label = element(text = "Save", clickableAncestor = button)
        val plain = element(text = "Plain")
        val clickable = element(text = "Direct", isClickable = true, clickableAncestor = button)
        val elements = listOf(button, label, plain, clickable)
        assertSame(button, findTapTarget(elements) { it.matches(text = "Save") })
        assertSame(plain, findTapTarget(elements) { it.matches(text = "Plain") })
        assertSame(clickable, findTapTarget(elements) { it.matches(text = "Direct") })
        assertNull(findTapTarget(elements) { it.matches(text = "Missing") })
        assertNull(findTapTarget(elements + label) { it.matches(text = "Save") })
    }
}
