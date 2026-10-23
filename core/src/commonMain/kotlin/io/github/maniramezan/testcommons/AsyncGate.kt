package io.github.maniramezan.testcommons

import kotlinx.coroutines.CompletableDeferred
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * A one-shot signal that resumes every waiter when opened.
 *
 * Opening is permanent and repeatable: later calls to [await] return immediately.
 * Cancelling a waiting coroutine removes only that waiter.
 *
 * ```kotlin
 * val gate = AsyncGate()
 * val job = launch { gate.await(); load() }
 * waitUntil(1.seconds, observe = { gate.waiterCount }) { it == 1 }
 * gate.open()
 * ```
 */
public class AsyncGate {
    private val signal = CompletableDeferred<Unit>()
    private val waiters = AtomicInt(0)

    /** The number of suspended callers, useful for establishing test interleavings. */
    public val waiterCount: Int get() = waiters.load()

    /** Whether [open] has been called. */
    public val isOpen: Boolean get() = signal.isCompleted

    /** Suspends until the gate opens, or throws when the calling coroutine is cancelled. */
    public suspend fun await() {
        if (signal.isCompleted) return
        waiters.incrementAndFetch()
        try {
            signal.await()
        } finally {
            waiters.decrementAndFetch()
        }
    }

    /** Opens the gate permanently and resumes every waiter. */
    public fun open() {
        signal.complete(Unit)
    }
}
