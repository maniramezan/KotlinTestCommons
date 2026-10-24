package io.github.maniramezan.testcommons.samples.android

import io.github.maniramezan.testcommons.AutomationId

internal object SampleIds {
    private val ids = AutomationId("sample")
    val name = ids.make("catalog", "profile", "name")
    val save = ids.make("catalog", "profile", "save")
    val greeting = ids.make("catalog", "profile", "greeting")
    val loading = ids.make("catalog", "lessons", "loading")
    val list = ids.make("catalog", "lessons", "list")
    val reload = ids.make("catalog", "lessons", "reload")
    fun lesson(index: Int): String = ids.make("catalog", "lessons", "item", key = index.toString())
}
