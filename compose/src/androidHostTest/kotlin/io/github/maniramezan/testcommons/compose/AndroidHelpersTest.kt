package io.github.maniramezan.testcommons.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

private val LocalThemeName = staticCompositionLocalOf { "none" }

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidHelpersTest {
    @get:Rule
    val composeRule = ThemedComposeRule(theme = { content ->
        CompositionLocalProvider(LocalThemeName provides "brand", content = content)
    })

    @Test
    fun deviceConfigurationAppliesEveryOverride() {
        var direction: LayoutDirection? = null
        var fontScale = 0f
        var dark = false
        var language = ""
        composeRule.setContent {
            val override = deviceConfiguration(
                locales = LocaleList("fa-IR"),
                layoutDirection = LayoutDirection.Rtl,
                fontScale = 2f,
                darkMode = true,
                size = DpSize(200.dp, 100.dp),
            )
            DeviceConfigurationOverride(override) {
                direction = LocalLayoutDirection.current
                fontScale = LocalDensity.current.fontScale
                dark = isSystemInDarkTheme()
                language = LocalConfiguration.current.locales[0].language
                Box(Modifier.fillMaxSize().testTag("content"))
            }
        }
        composeRule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, direction)
        assertEquals(2f, fontScale)
        assertEquals(true, dark)
        assertEquals("fa", language)
        composeRule.onNodeWithTag("content").assertWidthIsEqualTo(200.dp).assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun emptyConfigurationRendersContentUnchangedInTheTheme() {
        composeRule.setContent {
            DeviceConfigurationOverride(deviceConfiguration()) {
                BasicText("Theme: ${LocalThemeName.current}")
            }
        }
        composeRule.waitForText("Theme: brand")
    }

    @Test
    fun capturesPngBytes() {
        composeRule.setContent { Box(Modifier.size(10.dp).background(Color.Red).testTag("box")) }
        val png = composeRule.onNodeWithTag("box").captureToPng()
        assertContentEquals(
            byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()),
            png.copyOf(4),
        )
    }
}
