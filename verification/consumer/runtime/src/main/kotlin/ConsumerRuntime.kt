package io.github.maniramezan.testcommons.verification

import io.github.maniramezan.testcommons.CallRecorder

/** Verifies that a tool can ship the published core without loading a test framework. */
public fun main() {
    val recorder = CallRecorder<String>()
    recorder.record("resolved")
    check(recorder.values() == listOf("resolved"))
    for (name in listOf(
        "org.junit.Test",
        "org.junit.jupiter.api.Test",
        "kotlin.test.AssertionsKt",
        "kotlinx.coroutines.test.TestScope",
        "androidx.compose.runtime.Composer",
        "androidx.test.core.app.ActivityScenario",
    )) {
        check(runCatching { Class.forName(name) }.exceptionOrNull() is ClassNotFoundException) {
            "core unexpectedly supplied a test framework: $name"
        }
    }
}
