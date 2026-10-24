package io.github.maniramezan.testcommons.device

import android.os.Bundle
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PureLogicTest {
    @Test
    fun dragPathEndsAtTheDestination() {
        assertEquals(1, moveSteps(0))
        assertEquals(50, moveSteps(500))
        assertEquals(listOf(5f to 10f, 10f to 20f), dragPath(0, 0, 10, 20, 2))
    }

    @Test
    fun timeoutsConvertToNonnegativeMillis() {
        assertEquals(5_000L, waitMillis(DefaultDeviceTimeout))
        assertEquals(0L, waitMillis(Duration.ZERO))
        assertFailsWith<IllegalArgumentException> { waitMillis((-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { waitForView(isRoot(), (-1).milliseconds) }
    }

    @Test
    fun textReplacementVerification() {
        assertTrue(textReplaced("", "abc", "abc", isPassword = false))
        assertFalse(textReplaced("", "ab", "abc", isPassword = false))
        assertTrue(textReplaced("", "•••", "abc", isPassword = true))
        assertFalse(textReplaced("•••", "•••", "abc", isPassword = true))
        assertFalse(textReplaced("x", "", "abc", isPassword = true))
        assertTrue(textReplaced("x", "", "", isPassword = true))
    }

    @Test
    fun shellCommandsAreValidated() {
        assertEquals(
            "cmd locale set-app-locales com.example.app --locales fa-IR,en",
            appLocalesCommand("com.example.app", listOf("fa-IR", "en")),
        )
        assertEquals("cmd locale set-app-locales com.example --locales ", appLocalesCommand("com.example", emptyList()))
        assertFailsWith<IllegalArgumentException> { appLocalesCommand("com.example; reboot", listOf("en")) }
        assertFailsWith<IllegalArgumentException> { appLocalesCommand("example", listOf("en")) }
        assertFailsWith<IllegalArgumentException> { appLocalesCommand("com.example", listOf("en;rm")) }
        assertEquals(
            listOf(
                "settings put global window_animation_scale 0",
                "settings put global transition_animation_scale 0",
                "settings put global animator_duration_scale 0",
            ),
            animationCommands(enabled = false),
        )
        assertTrue(animationCommands(enabled = true).all { it.endsWith(" 1") })
    }

    @Test
    fun forwardsOnlyNamedNonEmptyArguments() {
        val forwarded = mutableMapOf<String, String>()
        val arguments = Bundle().apply {
            putString("BASE_URL", "http://10.0.2.2:8080")
            putString("EMPTY", "")
            putString("OTHER", "x")
        }
        forwardArguments(arguments, setOf("BASE_URL", "EMPTY", "MISSING")) { name, value -> forwarded[name] = value }
        assertEquals(mapOf("BASE_URL" to "http://10.0.2.2:8080"), forwarded)
        forwardArguments(null, setOf("BASE_URL")) { _, _ -> error("unexpected") }
    }

    @Test
    fun fileStemsAreFileSystemSafe() {
        assertEquals("loads_fa_IR_", fileStem("loads[fa/IR]"))
        assertEquals("test", fileStem(null))
    }
}
