package io.github.maniramezan.testcommons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScriptedResponderTest {
    @Test
    fun answersCallsInOrderAndRecordsRequests() =
        runTest {
            val responder = ScriptedResponder<String, Int>(listOf(Result.failure(TestError()), Result.success(42)))
            assertFailsWith<TestError> { responder.respond("first") }
            assertEquals(42, responder.respond("second"))
            assertEquals(listOf("first", "second"), responder.requests)
            assertEquals(2, responder.callCount)
        }

    @Test
    fun exhaustionIsDistinctFromScriptedFailures() =
        runTest {
            val responder = ScriptedResponder<Unit, Int>(listOf(Result.success(1)))
            responder.respond(Unit)
            assertEquals(1, assertFailsWith<ScriptedResponder.Exhausted> { responder.respond(Unit) }.callIndex)
            val withFallback = ScriptedResponder<Unit, Int>(emptyList(), fallback = Result.success(7))
            assertEquals(7, withFallback.respond(Unit))
        }

    @Test
    fun heldCallKeepsItsOutcomeWhileLaterCallsFinish() =
        runTest {
            val responder = ScriptedResponder<String, Int>(listOf(Result.success(1), Result.success(2)))
            val gate = responder.hold(call = 0)
            assertSame(gate, responder.hold(call = 0))
            val first = async { responder.respond("held") }
            runCurrent()
            assertFalse(first.isCompleted)
            assertEquals(listOf("held"), responder.requests)
            assertEquals(2, responder.respond("next"))
            gate.open()
            assertEquals(1, first.await())
            assertTrue(first.isCompleted)
            assertFailsWith<IllegalArgumentException> { responder.hold(-1) }
        }

    @Test
    fun waitsForCalls() =
        runTest {
            val responder = ScriptedResponder<Int, Int>(emptyList(), fallback = Result.success(0))
            responder.respond(5)
            val requests = withContext(Dispatchers.Default) { responder.waitForCalls(1) }
            assertEquals(listOf(5), requests)
        }
}
