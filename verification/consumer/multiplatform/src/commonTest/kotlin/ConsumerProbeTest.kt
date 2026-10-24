package io.github.maniramezan.testcommons.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsumerProbeTest {
    @Test
    fun loadsPublishedCoreAndComposeClasses() {
        assertEquals(listOf("resolved"), recordedValues())
        assertTrue(matcherDescription().contains("release."))
    }
}
