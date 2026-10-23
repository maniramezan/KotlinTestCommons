package io.github.maniramezan.testcommons.device

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * Matches views whose ID has the resource entry name [name], such as `save` for `R.id.save`.
 *
 * Use it when the test cannot reference the app's `R` class, as in generated tests.
 *
 * @param name The resource entry name.
 * @return The matcher.
 */
public fun hasResourceEntryName(name: String): Matcher<View> =
    object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with resource entry name: ").appendValue(name)
        }

        override fun matchesSafely(view: View): Boolean {
            if (view.id == View.NO_ID) return false
            return runCatching { view.resources.getResourceEntryName(view.id) == name }.getOrDefault(false)
        }
    }

/**
 * Matches only the [index]th view, in breadth-first order, among views matching [matcher].
 *
 * Stateless: the match list is recomputed from the view's hierarchy, so the matcher stays
 * correct when Espresso evaluates it many times, unlike a counting matcher.
 *
 * @param matcher Selects the candidates.
 * @param index The zero-based position among candidates.
 * @return The matcher.
 */
public fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
    require(index >= 0) { "index must be nonnegative" }
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("the match at index $index of ").appendDescriptionOf(matcher)
        }

        override fun matchesSafely(view: View): Boolean {
            if (!matcher.matches(view)) return false
            val matches = TreeIterables.breadthFirstViewTraversal(view.rootView).filter { matcher.matches(it) }
            return matches.getOrNull(index) === view
        }
    }
}

/**
 * Returns whether this view or a descendant is a `TextView` showing [expected], including text
 * inside `AndroidView` interop content that Compose semantics cannot see.
 *
 * @param expected The text to find.
 * @param substring Whether a partial match is enough.
 * @return `true` when a matching `TextView` exists.
 */
public fun View.containsText(expected: String, substring: Boolean = false): Boolean {
    if (this is TextView) {
        val actual = text?.toString().orEmpty()
        if (if (substring) expected in actual else actual == expected) return true
    }
    if (this !is ViewGroup) return false
    return (0 until childCount).any { getChildAt(it).containsText(expected, substring) }
}
