package io.github.maniramezan.testcommons.device

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DeviceHelpersTest {
    @get:Rule
    val diagnostics = DiagnosticsOnFailureRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val packageName = InstrumentationRegistry.getInstrumentation().context.packageName
    private lateinit var scenario: ActivityScenario<FixtureActivity>

    @Before
    fun launch() {
        scenario = ActivityScenario.launch(FixtureActivity::class.java)
        device.wait(androidx.test.uiautomator.Until.hasObject(By.text("Save")), 5_000)
    }

    @After
    fun close() {
        scenario.close()
    }

    @Test
    fun snapshotFindsTheScreenAndTapTargets() {
        val elements = AccessibilitySnapshot.capture(packageName = packageName)
        val save = assertNotNull(elements.firstOrNull { it.matches(id = "button1") })
        assertEquals("Save", save.label)
        assertTrue(save.isClickable)
        assertTrue(save.frame.width > 0)
        assertEquals(save, findTapTarget(elements) { it.matches(text = "Save") })
        assertTrue(elements.any { it.isPassword })
        assertTrue(AccessibilitySnapshot.capture(packageName = "com.example.absent").isEmpty())
    }

    @Test
    fun espressoWaitsForDelayedViews() {
        waitUntilDisplayed(withText("Loaded"))
        waitUntilNotDisplayed(withText("Missing"), timeoutMillis = 100)
        assertFailsWith<androidx.test.espresso.PerformException> {
            waitUntilDisplayed(withText("Missing"), timeoutMillis = 200)
        }
    }

    @Test
    fun objectQueriesRejectAmbiguityAndVerifyText() {
        assertEquals("Save", device.uniqueObject(By.text("Save")).text)
        assertFailsWith<AssertionError> { device.uniqueObject(By.clazz("android.widget.EditText")) }
        assertFailsWith<AssertionError> { device.uniqueObject(By.text("Missing"), timeoutMillis = 100) }
        device.setTextVerified(By.res("android:id/edit"), "Ada")
        device.setTextVerified(By.res("android:id/input"), "secret")
    }

    @Test
    fun scrollsUntilVisibleWithinItsBudget() {
        assertNotNull(device.scrollUntilVisible(By.text("Save"), maxScrolls = 0))
        assertNull(device.scrollUntilVisible(By.text("Item 59"), maxScrolls = 0))
        assertNull(
            device.scrollUntilVisible(By.text("Item 59"), container = By.res("missing-container"), maxScrolls = 1),
        )
        assertNotNull(device.findObject(By.text("Save")))
        assertNotNull(device.scrollUntilVisible(By.text("Item 59"), direction = Direction.DOWN, maxScrolls = 20))
        assertNotNull(
            device.scrollUntilVisible(
                By.text("Save"),
                container = By.scrollable(true),
                direction = Direction.UP,
                maxScrolls = 20,
            ),
        )
    }

    @Test
    fun gesturesAndBackNavigation() {
        val save = device.uniqueObject(By.text("Save")).visibleBounds
        assertTrue(device.dragWithHold(save.centerX(), save.centerY(), save.centerX(), save.centerY() + 50, 100, 0))
        assertTrue(device.pressBackReliably())
    }

    @Test
    fun diagnosticsAreWrittenOnFailure() {
        val directory = File(InstrumentationRegistry.getInstrumentation().context.cacheDir, "diagnostics-test")
        directory.deleteRecursively()
        val rule = DiagnosticsOnFailureRule(directory)
        val failing = object : Statement() {
            override fun evaluate(): Unit = throw IllegalStateException("boom")
        }
        assertFailsWith<IllegalStateException> {
            rule.apply(failing, Description.createTestDescription(DeviceHelpersTest::class.java, "fails[1]")).evaluate()
        }
        assertTrue(File(directory, "DeviceHelpersTest/fails_1_.png").length() > 0)
        assertTrue(File(directory, "DeviceHelpersTest/fails_1_.xml").readText().contains("Save"))
    }

    @Test
    fun readsInstrumentationArguments() {
        assertNull(instrumentationArgument("testcommons.absent"))
    }
}
