package io.github.maniramezan.testcommons.device

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Returns an instrumentation argument passed with `-e name value` or
 * `testInstrumentationRunnerArguments`, such as a mock server URL.
 *
 * @param name The argument name.
 * @return The value, or `null` when it was not passed.
 */
public fun instrumentationArgument(name: String): String? =
    InstrumentationRegistry.getArguments().getString(name)?.takeIf { it.isNotEmpty() }

/** Copies each named argument present in [arguments] to a system property of the same name. */
internal fun forwardArguments(arguments: Bundle?, names: Set<String>, setProperty: (String, String) -> Unit) {
    if (arguments == null) return
    for (name in names) {
        arguments.getString(name)?.takeIf { it.isNotEmpty() }?.let { setProperty(name, it) }
    }
}
