package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.await
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun checkFailure(test: () -> TestResult, check: (Throwable?) -> Unit): TestResult {
    val result = test().unsafeCast<Promise<JsAny?>>()
    return runTest { check(runCatching { result.await<JsAny?>() }.exceptionOrNull()) }
}
