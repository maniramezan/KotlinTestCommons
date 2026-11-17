package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.await
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.js.Promise

internal actual fun checkFailure(test: () -> TestResult, check: (Throwable?) -> Unit): TestResult {
    val result = test().unsafeCast<Promise<Unit>>()
    return runTest { check(runCatching { result.await() }.exceptionOrNull()) }
}
