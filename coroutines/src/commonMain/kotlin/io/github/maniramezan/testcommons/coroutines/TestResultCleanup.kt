package io.github.maniramezan.testcommons.coroutines

import kotlinx.coroutines.test.TestResult

/**
 * Starts [test] and runs [cleanup] once its [TestResult] has completed, whether it passed or failed.
 *
 * On the JVM, Android, and native targets the result is complete when [test] returns. On JS and
 * Wasm it is a promise, so [cleanup] waits until the promise settles.
 */
internal expect fun runThenCleanUp(test: () -> TestResult, cleanup: () -> Unit): TestResult
