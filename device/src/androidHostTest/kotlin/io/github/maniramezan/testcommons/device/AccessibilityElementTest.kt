package io.github.maniramezan.testcommons.device

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessibilityElementTest {
    private val save = element(text = "Save", contentDescription = "Save draft", resourceName = "com.example:id/save")

    @Test
    fun derivesLabelAndEntryName() {
        assertEquals("save", save.resourceEntryName)
        assertEquals("Save", save.label)
        assertEquals("Icon", element(contentDescription = "Icon").label)
        assertEquals("", element().resourceEntryName)
    }

    @Test
    fun matchesEverySelectorGiven() {
        assertTrue(save.matches())
        assertTrue(save.matches(id = "com.example:id/save"))
        assertTrue(save.matches(id = "save"))
        assertTrue(save.matches(id = "Save draft"))
        assertFalse(save.matches(id = "cancel"))
        assertTrue(save.matches(text = "Save"))
        assertTrue(save.matches(text = "Save draft"))
        assertFalse(save.matches(text = "save"))
        assertTrue(save.matches(containsText = "draft"))
        assertTrue(save.matches(containsText = "Sa"))
        assertFalse(save.matches(containsText = "Cancel"))
        assertFalse(save.matches(id = "save", text = "Cancel"))
    }
}
