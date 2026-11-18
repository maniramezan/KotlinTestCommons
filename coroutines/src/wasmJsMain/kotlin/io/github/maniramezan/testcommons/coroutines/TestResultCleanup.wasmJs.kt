package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.test.TestResult
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun runThenCleanUp(test: () -> TestResult, cleanup: () -> Unit): TestResult {
    val result = try {
        test()
    } catch (failure: Throwable) {
        cleanup()
        throw failure
    }
    // `TestResult` only exposes a `then` whose callbacks return nothing, so a rejection could not
    // be passed on unchanged; the underlying promise can return one.
    return result.unsafeCast<Promise<JsAny?>>().then(
        onFulfilled = { value ->
            cleanup()
            value
        },
        onRejected = { reason ->
            cleanup()
            Promise.reject(reason)
        },
    ).unsafeCast()
}
