package io.github.maniramezan.testcommons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutomationIdTest {
    private val ids = AutomationId("myapp")

    @Test
    fun joinsSegmentsWithDots() {
        assertEquals("myapp.home.feed.video_card", ids.make("home", "feed", "video_card"))
    }

    @Test
    fun percentEncodesKeysLikeIosAndAndroid() {
        assertEquals("myapp.home.feed.card.a%20b%2Ac~d-e.f_g", ids.make("home", "feed", "card", "a b*c~d-e.f_g"))
        assertEquals("%C3%A9%2F%25", AutomationId.encodeKey("é/%"))
    }

    @Test
    fun rejectsSegmentsThatAreNotLowercaseSnakeCase() {
        assertFailsWith<IllegalArgumentException> { AutomationId("MyApp") }
        assertFailsWith<IllegalArgumentException> { ids.make("Home", "feed", "card") }
        assertFailsWith<IllegalArgumentException> { ids.make("home", "1feed", "card") }
        assertFailsWith<IllegalArgumentException> { ids.make("home", "feed", "card-name") }
        assertTrue(AutomationId.isSegment("a1_b"))
        assertFalse(AutomationId.isSegment(""))
    }
}
