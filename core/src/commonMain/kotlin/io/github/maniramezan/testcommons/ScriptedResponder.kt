package io.github.maniramezan.testcommons

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A scripted fake that records each request and answers it with the next scripted outcome.
 *
 * Forward each call of an injected dependency to [respond]. Calls are numbered from zero in
 * arrival order, and call *n* always receives outcome *n*, even when an earlier call is held
 * and finishes later. [hold] returns a gate that suspends one call after it is recorded and
 * before it is answered, so a test can observe in-flight state deterministically.
 *
 * ```kotlin
 * val responder = ScriptedResponder<String, Int>(listOf(Result.failure(TestError()), Result.success(42)))
 * val fetch: suspend (String) -> Int = responder::respond
 * ```
 *
 * @param outcomes Successes and failures, indexed by call number.
 * @param fallback The outcome for calls after the script is used; `null` throws [Exhausted].
 */
public class ScriptedResponder<Request, Response>(
    outcomes: List<Result<Response>>,
    private val fallback: Result<Response>? = null,
) {
    /**
     * Thrown when a call arrives after every scripted outcome is used and no fallback exists.
     *
     * Distinct from scripted failures, so exhaustion cannot pass for an expected error.
     *
     * @property callIndex The zero-based index of the call that found the script empty.
     */
    public class Exhausted(public val callIndex: Int) :
        IllegalStateException("No scripted outcome for call $callIndex")

    private class State<Request>(val requests: List<Request>, val gates: Map<Int, AsyncGate>)

    private val outcomes = outcomes.toList()
    private val state = AtomicReference(State<Request>(emptyList(), emptyMap()))

    /** The recorded requests in arrival order, including calls still held or in flight. */
    public val requests: List<Request> get() = state.load().requests

    /** The number of calls received. */
    public val callCount: Int get() = state.load().requests.size

    /**
     * Returns a gate that holds the call with zero-based index [call] until opened.
     *
     * Register the hold before the call arrives. The call is recorded before it waits, and
     * cancelling the calling coroutine while it is held cancels the call.
     *
     * @param call The zero-based call index to hold.
     * @return The gate to open when the call may proceed.
     */
    public fun hold(call: Int): AsyncGate {
        require(call >= 0) { "call must be nonnegative" }
        val created = AsyncGate()
        val updated = state.updateAndFetch { current ->
            if (call in current.gates) current else State(current.requests, current.gates + (call to created))
        }
        return updated.gates.getValue(call)
    }

    /**
     * Records [request] and answers it with the next scripted outcome.
     *
     * @param request The request made to the faked dependency.
     * @return The scripted response.
     * @throws Exhausted when no outcome or fallback remains; otherwise the scripted failure.
     */
    public suspend fun respond(request: Request): Response {
        val previous = state.fetchAndUpdate { State(it.requests + request, it.gates) }
        val index = previous.requests.size
        previous.gates[index]?.await()
        val outcome = outcomes.getOrNull(index) ?: fallback ?: throw Exhausted(index)
        return outcome.getOrThrow()
    }

    /**
     * Waits in real time until at least [count] calls have been recorded.
     *
     * @param count The nonnegative minimum number of calls.
     * @param timeout The real-time observation budget.
     * @return A snapshot of the recorded requests.
     * @throws ObservationTimeout with the last snapshot when the deadline passes.
     */
    public suspend fun waitForCalls(count: Int, timeout: Duration = 2.seconds): List<Request> {
        require(count >= 0) { "count must be nonnegative" }
        return waitUntil(timeout, pollInterval = 1.milliseconds, observe = { requests }) { it.size >= count }
    }
}
