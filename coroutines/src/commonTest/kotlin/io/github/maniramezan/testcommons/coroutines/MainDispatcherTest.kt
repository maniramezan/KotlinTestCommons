package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class MainDispatcherTest {
    private val main = MainDispatcherFixture()

    @BeforeTest
    fun setUp() = main.install()

    @AfterTest
    fun tearDown() = main.reset()

    @Test
    fun fixtureRunsMainWorkOnTheSharedScheduler() = runTest(main.dispatcher) {
        var ran = false
        CoroutineScope(Dispatchers.Main).launch { ran = true }
        assertEquals(false, ran)
        testScheduler.advanceUntilIdle()
        assertEquals(true, ran)
    }

    @Test
    fun runTestWithMainSharesTheSchedulerWithMain() = runTestWithMain {
        var ran = false
        CoroutineScope(Dispatchers.Main).launch { ran = true }
        assertEquals(false, ran)
        testScheduler.advanceUntilIdle()
        assertEquals(true, ran)
    }

    @Test
    fun runTestWithMainAcceptsADispatcherAndTimeout() = runTestWithMain(UnconfinedTestDispatcher(), 5.seconds) {
        var ran = false
        CoroutineScope(Dispatchers.Main).launch { ran = true }
        assertEquals(true, ran)
    }

    @Test
    fun runTestWithMainRestoresMainAfterAFailure() {
        main.reset()
        assertFailsWith<IllegalStateException> {
            runTestWithMain(UnconfinedTestDispatcher()) {
                assertEquals(false, Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext))
                throw IllegalStateException("boom")
            }
        }
        val needsDispatch = runCatching { Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext) }
        assertEquals(true, needsDispatch.getOrDefault(true))
        main.install()
    }
}
