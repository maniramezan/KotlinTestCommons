package io.github.maniramezan.testcommons.device

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ObjectQueriesTest {
    @Test
    fun checksBeforeScrollingEvenWithNoBudget() {
        for (budget in listOf(-1, 0, 1)) {
            assertEquals("visible", scrollWithinBudget(budget, { "visible" }, { error("unexpected scroll") }))
        }
        assertNull(scrollWithinBudget(0, { null }, { error("unexpected scroll") }))
        assertNull(scrollWithinBudget(-1, { null }, { error("unexpected scroll") }))
    }

    @Test
    fun checksAfterTheLastScroll() {
        var scrolls = 0
        val target = scrollWithinBudget(3, { "target".takeIf { scrolls == 3 } }) {
            scrolls++
            true
        }
        assertEquals("target", target)
        assertEquals(3, scrolls)
    }

    @Test
    fun neverExceedsTheBudget() {
        var scrolls = 0
        assertNull(
            scrollWithinBudget(3, { null }) {
                scrolls++
                true
            },
        )
        assertEquals(3, scrolls)
    }

    @Test
    fun stopsWhenTheContainerIsMissingWithoutOverflowingTheBudget() {
        var scrolls = 0
        assertNull(
            scrollWithinBudget(Int.MAX_VALUE, { null }) {
                scrolls++
                false
            },
        )
        assertEquals(1, scrolls)
    }

    @Test
    fun unexpectedScrollFailuresPropagate() {
        assertFailsWith<IllegalStateException> {
            scrollWithinBudget(3, { null }) { error("scroll failed") }
        }
    }
}
