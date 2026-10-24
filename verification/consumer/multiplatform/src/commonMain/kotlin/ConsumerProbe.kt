package io.github.maniramezan.testcommons.verification

import io.github.maniramezan.testcommons.CallRecorder
import io.github.maniramezan.testcommons.compose.hasTestTagStartingWith

internal fun recordedValues(): List<String> {
    val recorder = CallRecorder<String>()
    recorder.record("resolved")
    return recorder.values()
}

internal fun matcherDescription(): String = hasTestTagStartingWith("release.").description
