package io.github.maniramezan.testcommons.samples.jvm

internal class GreetingCache(
    private val fetch: suspend (String) -> String,
    private val nowMillis: () -> Long,
    private val lifetimeMillis: Long = 1_000,
) {
    private data class Entry(val greeting: String, val expiresAt: Long)

    private val entries = mutableMapOf<String, Entry>()

    suspend fun greeting(name: String): String {
        val cached = entries[name]
        if (cached != null && nowMillis() < cached.expiresAt) return cached.greeting
        val greeting = fetch(name)
        entries[name] = Entry(greeting, nowMillis() + lifetimeMillis)
        return greeting
    }
}
