package io.github.maniramezan.testcommons.verification

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import io.github.maniramezan.testcommons.device.uniqueObject

internal fun queryDevice(device: UiDevice): UiObject2 = device.uniqueObject(By.text("Ready"))
