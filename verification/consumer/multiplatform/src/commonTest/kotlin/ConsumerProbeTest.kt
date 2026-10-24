package io.github.maniramezan.testcommons.verification

import io.github.maniramezan.testcommons.coroutines.runTestWithMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsumerProbeTest {
    @Test
    fun loadsPublishedCoreAndComposeClasses() {
        assertEquals(listOf("resolved"), recordedValues())
        assertTrue(matcherDescription().contains("release."))
    }

    @Test
    fun loadsPublishedCoroutinesHelpersOnEveryTarget() = runTestWithMain {
        withContext(Dispatchers.Main) {
            assertEquals(listOf("resolved"), recordedValues())
        }
    }
}
