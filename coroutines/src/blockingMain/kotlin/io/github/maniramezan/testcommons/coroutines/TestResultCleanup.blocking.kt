package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.test.TestResult

internal actual fun runThenCleanUp(test: () -> TestResult, cleanup: () -> Unit): TestResult {
    try {
        return test()
    } finally {
        cleanup()
    }
}
