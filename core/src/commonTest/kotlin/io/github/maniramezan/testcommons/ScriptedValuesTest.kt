package io.github.maniramezan.testcommons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScriptedValuesTest {
    @Test
    fun handsOutValuesInOrderThenFails() {
        val values = ScriptedValues(listOf(1, 2))
        assertEquals(2, values.remainingCount)
        assertEquals(1, values.next())
        assertEquals(2, values.next())
        assertEquals(0, values.remainingCount)
        assertEquals(2, assertFailsWith<ScriptedValues.Exhausted> { values.next() }.consumedCount)
        assertEquals(0, values.remainingCount)
    }

    @Test
    fun repeatLastRepeatsAndFailsWhenEmpty() {
        val values = ScriptedValues(listOf("a", "b"), ScriptedValues.Exhaustion.RepeatLast)
        assertEquals(listOf("a", "b", "b", "b"), List(4) { values.next() })
        val empty = ScriptedValues<String>(emptyList(), ScriptedValues.Exhaustion.RepeatLast)
        assertEquals(0, assertFailsWith<ScriptedValues.Exhausted> { empty.next() }.consumedCount)
    }

    @Test
    fun fallbackIsReturnedAfterExhaustion() {
        val values = ScriptedValues(listOf(1), ScriptedValues.Exhaustion.Fallback(0))
        assertEquals(listOf(1, 0, 0), List(3) { values.next() })
    }
}
