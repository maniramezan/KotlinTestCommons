package io.github.maniramezan.testcommons.device

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/**
 * An `AndroidJUnitRunner` that copies selected instrumentation arguments to system properties
 * before tests start, so app code that reads `System.getProperty` (for example a base URL
 * override) sees values passed with `-e` or `testInstrumentationRunnerArguments`.
 *
 * ```kotlin
 * class AppTestRunner : InstrumentationArgsRunner(setOf("MOCK_SERVER_BASE_URL"))
 * ```
 *
 * @param forwardedArguments The argument names to copy.
 */
public open class InstrumentationArgsRunner(private val forwardedArguments: Set<String>) : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        forwardArguments(arguments, forwardedArguments, System::setProperty)
        super.onCreate(arguments)
    }
}
