package io.github.maniramezan.testcommons.device

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import io.github.maniramezan.testcommons.Frame

/** The default wait budget for device helpers, in milliseconds. */
public const val DEFAULT_DEVICE_TIMEOUT_MILLIS: Long = 5_000L

/**
 * Waits for exactly one object matching [selector] and returns it.
 *
 * Fails loudly when the selector is ambiguous, instead of acting on whichever match comes first.
 *
 * @param selector Selects the object.
 * @param timeoutMillis The wait budget for at least one match.
 * @return The unique object.
 * @throws AssertionError when nothing matches in time or more than one object matches.
 */
public fun UiDevice.uniqueObject(selector: BySelector, timeoutMillis: Long = DEFAULT_DEVICE_TIMEOUT_MILLIS): UiObject2 {
    if (wait(Until.hasObject(selector), timeoutMillis) != true) {
        throw AssertionError("No object matches $selector after $timeoutMillis ms")
    }
    val matches = findObjects(selector)
    return matches.singleOrNull() ?: throw AssertionError(
        "Expected one object matching $selector but found ${matches.size}: " +
            matches.joinToString { describe(it) },
    )
}

/**
 * Replaces the text of the single field matching [selector] and verifies the result.
 *
 * Password fields report masked text, so for them this only verifies that the text changed.
 *
 * @param selector Selects one editable field.
 * @param text The new contents.
 * @param timeoutMillis The wait budget for the field to appear.
 * @throws AssertionError when the field is ambiguous, missing, or holds different text afterward.
 */
public fun UiDevice.setTextVerified(
    selector: BySelector,
    text: String,
    timeoutMillis: Long = DEFAULT_DEVICE_TIMEOUT_MILLIS,
) {
    val field = uniqueObject(selector, timeoutMillis)
    val before = field.text.orEmpty()
    val isPassword = field.isPassword()
    field.click()
    field.text = text
    waitForIdle()
    val after = field.text.orEmpty()
    if (!textReplaced(before, after, text, isPassword)) {
        throw AssertionError("Expected $selector to contain '${if (isPassword) "•".repeat(text.length) else text}'")
    }
}

/**
 * Scrolls until an object matching [target] is visible, within [maxScrolls] attempts.
 *
 * Scrolls [container] when given; otherwise the largest scrollable object on screen. Checks
 * before the first scroll and after the last one, and makes no scrolls for a nonpositive budget.
 *
 * @param target Selects the object to reveal.
 * @param container Selects the object to scroll, or `null` to pick one.
 * @param direction The direction to scroll the content, such as [Direction.DOWN] to reveal content below.
 * @param maxScrolls The scroll budget.
 * @return The object, or `null` when it is not visible after the budget is used.
 */
public fun UiDevice.scrollUntilVisible(
    target: BySelector,
    container: BySelector? = null,
    direction: Direction = Direction.DOWN,
    maxScrolls: Int = 10,
): UiObject2? {
    repeat(maxScrolls.coerceAtLeast(0) + 1) { attempt ->
        findObject(target)?.let { return it }
        if (attempt == maxScrolls.coerceAtLeast(0)) return null
        val scroller = container?.let(::findObject)
            ?: findObjects(By.scrollable(true))
                .maxByOrNull { it.visibleBounds.width() * it.visibleBounds.height() }
            ?: return null
        // `scroll` reports `false` whenever it sees no scroll event in time, which slow devices
        // can report mid-list, so only the budget ends the search.
        runCatching { scroller.scroll(direction, 0.8f) }
        waitForIdle()
    }
    return null
}

/**
 * Returns whether a text replacement succeeded: exact text for plain fields; for password fields,
 * which report masks, any change to nonempty content.
 */
internal fun textReplaced(before: String, after: String, expected: String, isPassword: Boolean): Boolean =
    after == expected || (isPassword && expected.isNotEmpty() && after.isNotEmpty() && after != before)

private fun UiObject2.isPassword(): Boolean {
    val bounds = runCatching { visibleBounds }.getOrNull() ?: return false
    val frame = Frame(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
    return AccessibilitySnapshot.capture().any { it.isPassword && it.frame == frame }
}

private fun describe(element: UiObject2): String =
    runCatching {
        "${element.className}(id=${element.resourceName}, text=${element.text}, desc=${element.contentDescription})"
    }
        .getOrDefault("<stale>")
