package io.github.maniramezan.testcommons.verification

import io.github.maniramezan.testcommons.junit.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AndroidDispatcherTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun loadsThePublishedJunitAdapterOnAndroid() = runTest(mainDispatcher.dispatcher) {
        withContext(Dispatchers.Main) {
            assertEquals(listOf("resolved"), recordedValues())
        }
    }
}
