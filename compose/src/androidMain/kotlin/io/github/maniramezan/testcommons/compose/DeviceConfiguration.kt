package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.then
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Combines the device settings a UI test most often varies into one override; `null` leaves a
 * setting unchanged.
 *
 * Apply it around the content under test, so one test class can cover right-to-left
 * layouts, large text, and dark mode without changing the emulator:
 *
 * ```kotlin
 * composeRule.setContent {
 *     DeviceConfigurationOverride(deviceConfiguration(locales = LocaleList("fa"), fontScale = 2f)) {
 *         AppTheme { Calendar() }
 *     }
 * }
 * ```
 *
 * @param locales The locales to report, such as `LocaleList("fa-IR")`.
 * @param layoutDirection The layout direction; defaults to the locale's direction when `null`.
 * @param fontScale The font scale, such as `2f` for 200%.
 * @param darkMode Whether the system is in dark mode.
 * @param size A forced size for the content.
 * @return The combined override.
 */
public fun deviceConfiguration(
    locales: LocaleList? = null,
    layoutDirection: LayoutDirection? = null,
    fontScale: Float? = null,
    darkMode: Boolean? = null,
    size: DpSize? = null,
): DeviceConfigurationOverride {
    val overrides = listOfNotNull(
        locales?.let { DeviceConfigurationOverride.Locales(it) },
        layoutDirection?.let { DeviceConfigurationOverride.LayoutDirection(it) },
        fontScale?.let { DeviceConfigurationOverride.FontScale(it) },
        darkMode?.let { DeviceConfigurationOverride.DarkMode(it) },
        size?.let { DeviceConfigurationOverride.ForcedSize(it) },
    )
    return overrides.fold(DeviceConfigurationOverride { content -> content() }) { combined, next -> combined then next }
}
