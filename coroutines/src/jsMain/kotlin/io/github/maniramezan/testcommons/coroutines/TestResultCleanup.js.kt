package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.test.TestResult

internal actual fun runThenCleanUp(test: () -> TestResult, cleanup: () -> Unit): TestResult {
    val result = try {
        test()
    } catch (failure: Throwable) {
        cleanup()
        throw failure
    }
    return result.then(
        onFulfilled = { cleanup() },
        onRejected = { failure ->
            cleanup()
            throw failure
        },
    )
}
