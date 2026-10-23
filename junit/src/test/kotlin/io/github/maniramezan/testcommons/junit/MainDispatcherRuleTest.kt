package io.github.maniramezan.testcommons.junit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun mainRunsOnTheSharedScheduler() =
        runTest(mainDispatcher.dispatcher) {
            var ran = false
            launch(Dispatchers.Main) { ran = true }
            testScheduler.advanceUntilIdle()
            assertTrue(ran)
        }

    @Test
    fun resetsMainWhenTheTestFails() {
        val rule = MainDispatcherRule(UnconfinedTestDispatcher())
        val failing = object : Statement() {
            override fun evaluate() {
                assertEquals(Dispatchers.Main.isDispatchNeeded(kotlin.coroutines.EmptyCoroutineContext), false)
                throw IllegalStateException("boom")
            }
        }
        assertFailsWith<IllegalStateException> {
            rule.apply(failing, Description.createTestDescription(javaClass, "failing")).evaluate()
        }
        assertFailsWith<IllegalStateException> {
            Dispatchers.Main.isDispatchNeeded(kotlin.coroutines.EmptyCoroutineContext)
        }
    }
}
