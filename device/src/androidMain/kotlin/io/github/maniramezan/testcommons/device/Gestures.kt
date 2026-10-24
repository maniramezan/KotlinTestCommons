package io.github.maniramezan.testcommons.device

import android.accessibilityservice.AccessibilityService
import android.app.UiAutomation
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/** The interval between injected drag moves, in milliseconds. */
private const val MOVE_INTERVAL_MILLIS = 10L

/** The pause at the destination before release, so drop targets register the drag's end. */
private const val DROP_SETTLE_MILLIS = 200L

/**
 * Performs a real drag: presses at the origin, holds for [holdMillis], moves to the destination
 * over [durationMillis], settles, and releases.
 *
 * `UiDevice.drag` and `swipe` start moving immediately, which reads as a fling; a hold is what
 * puts reorderable lists and drag-and-drop targets into their drag state.
 *
 * @param fromX The start x coordinate, in screen pixels.
 * @param fromY The start y coordinate, in screen pixels.
 * @param toX The end x coordinate, in screen pixels.
 * @param toY The end y coordinate, in screen pixels.
 * @param durationMillis The time spent moving.
 * @param holdMillis The time held at the origin before moving.
 * @param uiAutomation The automation connection that injects events.
 * @return `true` when every event was injected.
 */
public fun UiDevice.dragWithHold(
    fromX: Int,
    fromY: Int,
    toX: Int,
    toY: Int,
    durationMillis: Long = 500,
    holdMillis: Long = 600,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): Boolean {
    require(durationMillis >= 0 && holdMillis >= 0) { "durations must be nonnegative" }
    val downTime = SystemClock.uptimeMillis()
    if (!uiAutomation.inject(MotionEvent.ACTION_DOWN, downTime, fromX.toFloat(), fromY.toFloat())) return false
    if (holdMillis > 0) SystemClock.sleep(holdMillis)
    for ((x, y) in dragPath(fromX, fromY, toX, toY, moveSteps(durationMillis))) {
        if (!uiAutomation.inject(MotionEvent.ACTION_MOVE, downTime, x, y)) return false
        SystemClock.sleep(MOVE_INTERVAL_MILLIS)
    }
    SystemClock.sleep(DROP_SETTLE_MILLIS)
    val released = uiAutomation.inject(MotionEvent.ACTION_UP, downTime, toX.toFloat(), toY.toFloat())
    waitForIdle()
    return released
}

/**
 * Navigates back and reports whether the system accepted the action.
 *
 * `UiDevice.pressBack()` reports failure when no content-change event follows within about a
 * second, which Compose navigation often doesn't emit, so a successful back can read as a
 * failure. This dispatches the global back action and falls back to the back key.
 *
 * @param uiAutomation The automation connection that dispatches the action.
 * @return `true` when back was dispatched.
 */
public fun UiDevice.pressBackReliably(
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): Boolean {
    waitForIdle()
    val dispatched = runCatching { uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }
        .getOrDefault(false)
    if (dispatched) {
        waitForIdle()
        return true
    }
    return pressKeyCode(KeyEvent.KEYCODE_BACK)
}

/**
 * Returns whether the on-screen keyboard is shown, from the window list rather than shell output.
 *
 * Enables interactive-window retrieval, which stays on for the rest of the test process.
 *
 * @param uiAutomation The automation connection that lists the windows.
 * @return `true` when an input method window is on screen.
 */
public fun UiDevice.isKeyboardVisible(
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): Boolean {
    enableInteractiveWindows(uiAutomation)
    val windows = runCatching { uiAutomation.windows }.getOrNull().orEmpty()
    try {
        return hasInputMethodWindow(windows.map { it.type })
    } finally {
        @Suppress("DEPRECATION")
        for (window in windows) runCatching { window.recycle() }
    }
}

/** Returns whether any of the [windowTypes] is an input method window. */
internal fun hasInputMethodWindow(windowTypes: List<Int>): Boolean =
    windowTypes.any { it == AccessibilityWindowInfo.TYPE_INPUT_METHOD }

/** Returns the number of move events for a drag lasting [durationMillis]; at least one. */
internal fun moveSteps(durationMillis: Long): Int = (durationMillis / MOVE_INTERVAL_MILLIS).toInt().coerceAtLeast(1)

/** Returns evenly spaced points after the start, ending exactly at the destination. */
internal fun dragPath(fromX: Int, fromY: Int, toX: Int, toY: Int, steps: Int): List<Pair<Float, Float>> =
    (1..steps).map { step ->
        val progress = step.toFloat() / steps
        (fromX + (toX - fromX) * progress) to (fromY + (toY - fromY) * progress)
    }

private fun UiAutomation.inject(action: Int, downTime: Long, x: Float, y: Float): Boolean {
    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
    // Injection is rejected unless the event claims a touchscreen source.
    event.source = InputDevice.SOURCE_TOUCHSCREEN
    return try {
        injectInputEvent(event, true)
    } finally {
        event.recycle()
    }
}
