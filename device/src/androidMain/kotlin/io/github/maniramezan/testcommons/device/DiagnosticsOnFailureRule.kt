package io.github.maniramezan.testcommons.device

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * Saves a screenshot and the window hierarchy when a test fails, so a CI failure shows what was
 * on screen.
 *
 * Files are written to `<directory>/<TestClass>/<testMethod>.png` and `.xml`. The default
 * directory is in the instrumentation app's external files, which `adb pull` can read.
 * Capturing never masks the test's own failure.
 *
 * ```kotlin
 * @get:Rule val diagnostics = DiagnosticsOnFailureRule()
 * ```
 *
 * @property directory Where to write diagnostics.
 */
public class DiagnosticsOnFailureRule(public val directory: File = defaultDirectory()) : TestWatcher() {
    override fun failed(e: Throwable, description: Description) {
        runCatching {
            val folder = File(directory, description.testClass?.simpleName ?: "UnknownTest").apply { mkdirs() }
            val name = fileStem(description.methodName)
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.takeScreenshot(File(folder, "$name.png"))
            device.dumpWindowHierarchy(File(folder, "$name.xml"))
        }
    }

    private companion object {
        fun defaultDirectory(): File {
            val context = InstrumentationRegistry.getInstrumentation().context
            return File(context.getExternalFilesDir(null) ?: context.filesDir, "test-diagnostics")
        }
    }
}

/** Returns a file-system-safe name for a test method, including parameterized names. */
internal fun fileStem(methodName: String?): String =
    methodName.orEmpty().ifEmpty { "test" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
