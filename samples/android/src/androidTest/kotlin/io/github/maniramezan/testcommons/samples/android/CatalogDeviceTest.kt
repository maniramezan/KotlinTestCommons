package io.github.maniramezan.testcommons.samples.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import io.github.maniramezan.testcommons.device.AccessibilitySnapshot
import io.github.maniramezan.testcommons.device.DiagnosticsOnFailureRule
import io.github.maniramezan.testcommons.device.isKeyboardVisible
import io.github.maniramezan.testcommons.device.launchTargetApp
import io.github.maniramezan.testcommons.device.pressBackReliably
import io.github.maniramezan.testcommons.device.scrollUntilVisible
import io.github.maniramezan.testcommons.device.setTextVerified
import io.github.maniramezan.testcommons.device.uniqueObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CatalogDeviceTest {
    @get:Rule
    val diagnostics = DiagnosticsOnFailureRule()

    @Test
    fun launchesTheAppAndUsesTheSameIdsFromUiAutomator() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        launchTargetApp().use {
            device.setTextVerified(By.res(SampleIds.name), "Grace")
            if (device.isKeyboardVisible()) device.pressBackReliably()
            device.uniqueObject(By.res(SampleIds.save)).click()
            device.uniqueObject(By.text("Hello, Grace!"))
            device.uniqueObject(By.res(SampleIds.list))
            assertNotNull(device.scrollUntilVisible(By.res(SampleIds.lesson(40)), maxScrolls = 30))
            val elements = AccessibilitySnapshot.capture(
                packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName,
            )
            assertTrue(elements.any { it.matches(id = SampleIds.lesson(40)) })
        }
    }
}
