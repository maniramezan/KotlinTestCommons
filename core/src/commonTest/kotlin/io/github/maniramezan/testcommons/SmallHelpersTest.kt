package io.github.maniramezan.testcommons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SmallHelpersTest {
    @Test
    fun fixtureEnvironmentReplacesAndRemovesWithoutMutatingTheBase() {
        val base = mapOf("A" to "1", "B" to "2")
        val result = fixtureEnvironment(base, mapOf("A" to "x", "B" to null, "C" to "3"))
        assertEquals(mapOf("A" to "x", "C" to "3"), result)
        assertEquals(mapOf("A" to "1", "B" to "2"), base)
        assertEquals(mapOf("K" to "v"), fixtureEnvironment(overrides = mapOf("K" to "v")))
    }

    @Test
    fun roundTripUsesTheCallersCodec() {
        assertEquals(42, roundTrip(42, { it.toString() }, { it.toInt() }))
    }

    @Test
    fun testErrorsAreEqual() {
        assertEquals<Throwable>(TestError(), TestError())
        assertEquals(TestError().hashCode(), TestError().hashCode())
        assertNotEquals<Any>(TestError(), IllegalStateException())
    }

    @Test
    fun observationTimeoutDescribesTheLastObservation() {
        val timeout = ObservationTimeout(listOf(1, 2))
        assertEquals(listOf(1, 2), timeout.lastObservation)
        assertTrue(timeout.message.orEmpty().endsWith("[1, 2]"))
    }
}
