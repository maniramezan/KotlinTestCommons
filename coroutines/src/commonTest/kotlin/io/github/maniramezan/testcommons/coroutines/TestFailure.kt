package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.test.TestResult

/** Starts [test], then passes its failure, or `null` when it passed, to [check] once it has completed. */
internal expect fun checkFailure(test: () -> TestResult, check: (Throwable?) -> Unit): TestResult
