package io.github.maniramezan.testcommons.device

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.StringDescription
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ViewMatchersTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun matchesResourceEntryNames() {
        val view = View(context).apply { id = android.R.id.button1 }
        assertTrue(hasResourceEntryName("button1").matches(view))
        assertFalse(hasResourceEntryName("button2").matches(view))
        assertFalse(hasResourceEntryName("button1").matches(View(context)))
        assertFalse(hasResourceEntryName("x").matches(View(context).apply { id = 0x7f999999 }))
        assertTrue(StringDescription.toString(hasResourceEntryName("button1")).contains("button1"))
    }

    @Test
    fun withIndexSelectsOneMatchInBreadthFirstOrder() {
        val first = TextView(context).apply { text = "Item" }
        val second = TextView(context).apply { text = "Item" }
        val root = LinearLayout(context).apply {
            addView(first)
            addView(FrameLayout(context).apply { addView(second) })
        }
        assertTrue(withIndex(withText("Item"), 0).matches(first))
        assertFalse(withIndex(withText("Item"), 0).matches(second))
        assertTrue(withIndex(withText("Item"), 1).matches(second))
        assertFalse(withIndex(withText("Item"), 1).matches(root))
        assertTrue(StringDescription.toString(withIndex(withText("Item"), 1)).contains("index 1"))
        assertFailsWith<IllegalArgumentException> { withIndex(withText("Item"), -1) }
    }

    @Test
    fun containsTextSearchesDescendants() {
        val root = LinearLayout(context).apply {
            addView(FrameLayout(context).apply { addView(TextView(context).apply { text = "Markdown body" }) })
            addView(Button(context).apply { text = "OK" })
        }
        assertTrue(root.containsText("OK"))
        assertTrue(root.containsText("body", substring = true))
        assertFalse(root.containsText("body"))
        assertFalse(View(context).containsText("OK"))
    }
}
