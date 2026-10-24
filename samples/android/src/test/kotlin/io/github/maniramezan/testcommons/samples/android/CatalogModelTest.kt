package io.github.maniramezan.testcommons.samples.android

import io.github.maniramezan.testcommons.AsyncGate
import io.github.maniramezan.testcommons.ScriptedResponder
import io.github.maniramezan.testcommons.TestError
import io.github.maniramezan.testcommons.junit.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CatalogModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun loadingRemainsVisibleUntilTheRequestFinishes() = runTest(mainDispatcher.dispatcher) {
        val responses = ScriptedResponder<Unit, List<String>>(listOf(Result.success(listOf("Lesson 1"))))
        val gate = responses.hold(0)
        val model = CatalogModel { responses.respond(Unit) }
        val loading = async(Dispatchers.Main) { model.load() }
        runCurrent()
        assertTrue(model.state.value.isLoading)
        assertFalse(loading.isCompleted)
        assertEquals(1, gate.waiterCount)
        gate.open()
        loading.await()
        assertEquals(CatalogState(isLoading = false, lessons = listOf("Lesson 1")), model.state.value)
    }

    @Test
    fun retriesAfterAnError() = runTest(mainDispatcher.dispatcher) {
        val responses = ScriptedResponder<Unit, List<String>>(
            listOf(Result.failure(TestError()), Result.success(listOf("Lesson 1"))),
        )
        val model = CatalogModel { responses.respond(Unit) }
        model.load()
        assertEquals("Could not load lessons. Try again.", model.state.value.error)
        model.load()
        assertEquals(CatalogState(isLoading = false, lessons = listOf("Lesson 1")), model.state.value)
        assertEquals(2, responses.callCount)
    }

    @Test
    fun cancellationPropagatesWithoutBecomingAnError() = runTest(mainDispatcher.dispatcher) {
        val gate = AsyncGate()
        val model = CatalogModel {
            gate.await()
            emptyList()
        }
        val loading = async { model.load() }
        runCurrent()
        loading.cancel()
        assertFailsWith<CancellationException> { loading.await() }
        assertEquals(CatalogState(), model.state.value)
        assertEquals(0, gate.waiterCount)
    }
}
