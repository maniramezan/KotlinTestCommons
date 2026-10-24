package io.github.maniramezan.testcommons.verification

import io.github.maniramezan.testcommons.junit.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Junit4ConsumerTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun runsWithoutJupiterOnTheClasspath() = runTest(mainDispatcher.dispatcher) {
        assertFailsWith<ClassNotFoundException> {
            Class.forName("org.junit.jupiter.api.extension.Extension")
        }
        assertEquals("Main", withContext(Dispatchers.Main) { "Main" })
    }
}
