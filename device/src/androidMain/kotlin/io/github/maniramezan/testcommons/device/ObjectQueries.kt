package io.github.maniramezan.testcommons.device

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import io.github.maniramezan.testcommons.Frame
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The default wait budget for device helpers, in milliseconds. */
public val DefaultDeviceTimeout: Duration = 5.seconds

/**
 * Waits for exactly one object matching [selector] and returns it.
 *
 * Fails loudly when the selector is ambiguous, instead of acting on whichever match comes first.
 *
 * @param selector Selects the object.
 * @param timeout The nonnegative wait budget for at least one match.
 * @return The unique object.
 * @throws AssertionError when nothing matches in time or more than one object matches.
 */
public fun UiDevice.uniqueObject(selector: BySelector, timeout: Duration = DefaultDeviceTimeout): UiObject2 {
    if (wait(Until.hasObject(selector), waitMillis(timeout)) != true) {
        throw AssertionError("No object matches $selector after $timeout")
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
 * Password fields report masked text, so for them this only verifies that the text changed. The
 * accessibility tree is read to detect a password field only when the text doesn't already match.
 *
 * @param selector Selects one editable field.
 * @param text The new contents.
 * @param timeout The nonnegative wait budget for the field to appear.
 * @throws AssertionError when the field is ambiguous, missing, or holds different text afterward.
 */
public fun UiDevice.setTextVerified(
    selector: BySelector,
    text: String,
    timeout: Duration = DefaultDeviceTimeout,
) {
    val field = uniqueObject(selector, timeout)
    val before = field.text.orEmpty()
    field.click()
    field.text = text
    waitForIdle()
    val after = field.text.orEmpty()
    val isPassword by lazy(LazyThreadSafetyMode.NONE) { field.isPassword() }
    if (!textReplaced(before, after, text) { isPassword }) {
        throw AssertionError("Expected $selector to contain '${if (isPassword) "•".repeat(text.length) else text}'")
    }
}

/**
 * Scrolls until an object matching [target] is visible, within [maxScrolls] attempts.
 *
 * Scrolls [container] when given; otherwise the largest scrollable object on screen. Checks
 * before the first scroll and after the last one, and makes no scrolls for a nonpositive budget.
 * A missing explicit container ends the search. A scroll's event result does not end the search;
 * transiently stale containers are looked up again on the next attempt.
 * Gestures stay 20% inside each container edge to avoid system bars and app chrome.
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
): UiObject2? = scrollWithinBudget(
    maxScrolls = maxScrolls,
    findTarget = { findObject(target) },
    scroll = {
        val scroller = if (container != null) {
            findObject(container)
        } else {
            findObjects(By.scrollable(true))
                .maxByOrNull { it.visibleBounds.width().toLong() * it.visibleBounds.height() }
        }
        if (scroller == null) {
            false
        } else {
            try {
                scroller.setGestureMarginPercentage(0.2f)
                scroller.scroll(direction, 0.8f)
            } catch (_: StaleObjectException) {
            }
            waitForIdle()
            true
        }
    },
)

internal fun waitMillis(timeout: Duration): Long {
    require(!timeout.isNegative()) { "timeout must be nonnegative" }
    return timeout.inWholeMilliseconds
}

internal fun <T> scrollWithinBudget(maxScrolls: Int, findTarget: () -> T?, scroll: () -> Boolean): T? {
    findTarget()?.let { return it }
    repeat(maxScrolls.coerceAtLeast(0)) {
        if (!scroll()) return null
        findTarget()?.let { return it }
    }
    return null
}

/**
 * Returns whether a text replacement succeeded: exact text for plain fields; for password fields,
 * which report masks, any change to nonempty content. [isPassword] is read only when the text
 * differs from [expected] and the content changed.
 */
internal fun textReplaced(before: String, after: String, expected: String, isPassword: () -> Boolean): Boolean =
    after == expected ||
        (expected.isNotEmpty() && after.isNotEmpty() && after != before && isPassword())

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
