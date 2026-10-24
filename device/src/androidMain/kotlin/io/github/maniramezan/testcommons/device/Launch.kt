package io.github.maniramezan.testcommons.device

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.time.Duration

/**
 * Launches the instrumented app's launcher activity in a fresh task.
 *
 * Use it in a test that instruments the app under test; close the scenario when done.
 *
 * @return The scenario for the launched activity.
 * @throws IllegalStateException when the app has no launcher activity.
 */
public fun launchTargetApp(): ActivityScenario<Activity> {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) {
        "No launcher activity found for ${context.packageName}"
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return ActivityScenario.launch(intent)
}

/**
 * Launches another installed app's launcher activity in a fresh task and waits for its window.
 *
 * Use it from UiAutomator tests that drive an app they do not instrument.
 *
 * @param packageName The app to launch.
 * @param timeout The nonnegative wait budget for the app's window to appear.
 * @throws IllegalStateException when the app has no launcher activity.
 * @throws AssertionError when the app's window does not appear in time.
 */
public fun UiDevice.launchPackage(packageName: String, timeout: Duration = DefaultDeviceTimeout) {
    val context = InstrumentationRegistry.getInstrumentation().context
    val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(packageName)) {
        "No launcher activity found for $packageName"
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    if (wait(Until.hasObject(By.pkg(packageName).depth(0)), waitMillis(timeout)) != true) {
        throw AssertionError("$packageName did not show a window within $timeout")
    }
}
