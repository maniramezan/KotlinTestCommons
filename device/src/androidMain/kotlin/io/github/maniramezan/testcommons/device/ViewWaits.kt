package io.github.maniramezan.testcommons.device

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

/** The pause between checks while waiting, in milliseconds. */
private const val POLL_INTERVAL_MILLIS = 50L

/**
 * Returns an action, performed on the root view, that waits until a view matching [matcher] is
 * displayed ([displayed] `true`) or until none is ([displayed] `false`).
 *
 * The main thread keeps running between checks, so the app makes progress while the test waits.
 *
 * @param matcher Selects the view.
 * @param timeout The nonnegative wait budget.
 * @param displayed Whether to wait for the view to be displayed or to be gone.
 * @return The action; perform it with `onView(isRoot()).perform(action)`.
 * @throws IllegalArgumentException when [timeout] is negative.
 */
public fun waitForView(
    matcher: Matcher<View>,
    timeout: Duration = DefaultDeviceTimeout,
    displayed: Boolean = true,
): ViewAction {
    val timeoutMillis = waitMillis(timeout)
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()

        override fun getDescription(): String =
            if (displayed) {
                "wait up to $timeout for a displayed view matching $matcher"
            } else {
                "wait up to $timeout for no displayed view matching $matcher"
            }

        override fun perform(uiController: UiController, view: View) {
            val deadline = SystemClock.uptimeMillis() + timeoutMillis
            do {
                uiController.loopMainThreadUntilIdle()
                val found = TreeIterables.breadthFirstViewTraversal(view).any {
                    matcher.matches(it) && isDisplayed().matches(it)
                }
                if (found == displayed) return
                uiController.loopMainThreadForAtLeast(POLL_INTERVAL_MILLIS)
            } while (SystemClock.uptimeMillis() < deadline)
            throw PerformException.Builder()
                .withActionDescription(description)
                .withViewDescription(HumanReadables.describe(view))
                .withCause(TimeoutException(description))
                .build()
        }
    }
}

/**
 * Waits until a view matching [matcher] is displayed.
 *
 * @param matcher Selects the view.
 * @param timeout The nonnegative wait budget.
 * @throws PerformException when no matching view is displayed in time.
 */
public fun waitUntilDisplayed(matcher: Matcher<View>, timeout: Duration = DefaultDeviceTimeout) {
    onView(isRoot()).perform(waitForView(matcher, timeout, displayed = true))
}

/**
 * Waits until no view matching [matcher] is displayed.
 *
 * @param matcher Selects the view.
 * @param timeout The nonnegative wait budget.
 * @throws PerformException when a matching view is still displayed.
 */
public fun waitUntilNotDisplayed(matcher: Matcher<View>, timeout: Duration = DefaultDeviceTimeout) {
    onView(isRoot()).perform(waitForView(matcher, timeout, displayed = false))
}
