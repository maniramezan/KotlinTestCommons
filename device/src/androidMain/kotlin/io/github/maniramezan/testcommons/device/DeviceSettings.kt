package io.github.maniramezan.testcommons.device

import androidx.test.uiautomator.UiDevice

private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
private val LANGUAGE_TAG = Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$")
private val ANIMATION_SCALES = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")

/**
 * Sets the per-app languages of [packageName], like choosing an app language in Settings.
 *
 * The XCUITest counterpart passes `-AppleLanguages` at launch; on Android 13+ the system stores
 * per-app locales instead. Relaunch the app afterward. An empty list restores the system locale.
 *
 * @param packageName The app whose locales to set.
 * @param languageTags BCP 47 tags in preference order, such as `listOf("fa-IR")`.
 * @throws IllegalArgumentException for an invalid package name or language tag.
 * @throws UnsupportedOperationException below Android 13.
 */
public fun UiDevice.setAppLocales(packageName: String, languageTags: List<String>) {
    val command = appLocalesCommand(packageName, languageTags)
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        throw UnsupportedOperationException("Per-app locales need Android 13 (API 33) or later")
    }
    executeShellCommand(command)
}

/**
 * Turns the system window, transition, and animator animations off or back on, so UI tests do
 * not wait on, or race with, animations. Restore them when the suite finishes.
 *
 * @param enabled Whether animations run at normal speed.
 */
public fun UiDevice.setAnimationsEnabled(enabled: Boolean) {
    for (command in animationCommands(enabled)) executeShellCommand(command)
}

internal fun appLocalesCommand(packageName: String, languageTags: List<String>): String {
    require(PACKAGE_NAME.matches(packageName)) { "Invalid package name: $packageName" }
    languageTags.forEach { require(LANGUAGE_TAG.matches(it)) { "Invalid language tag: $it" } }
    return "cmd locale set-app-locales $packageName --locales ${languageTags.joinToString(",")}"
}

internal fun animationCommands(enabled: Boolean): List<String> {
    val scale = if (enabled) "1" else "0"
    return ANIMATION_SCALES.map { "settings put global $it $scale" }
}
