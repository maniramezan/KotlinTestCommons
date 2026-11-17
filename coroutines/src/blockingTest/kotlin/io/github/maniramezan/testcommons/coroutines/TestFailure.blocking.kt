package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.test.TestResult

internal actual fun checkFailure(test: () -> TestResult, check: (Throwable?) -> Unit): TestResult =
    check(runCatching(test).exceptionOrNull())
