package io.github.maniramezan.testcommons.samples.jvm

import io.github.maniramezan.testcommons.ManualClock
import io.github.maniramezan.testcommons.ScriptedResponder
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/** Runs a deterministic cache-expiry walkthrough without a network or real-time sleeps. */
public fun main(): Unit = runBlocking {
    val clock = ManualClock()
    val responses = ScriptedResponder<String, String>(
        listOf(Result.success("Hello, Ada!"), Result.success("Welcome back, Ada!")),
    )
    val cache = GreetingCache(responses::respond, clock::currentTimeMillis)
    println("First request: ${cache.greeting("Ada")}")
    println("Cached request: ${cache.greeting("Ada")}")
    clock.advanceBy(1.seconds)
    println("After expiry: ${cache.greeting("Ada")}")
    println("Upstream requests: ${responses.requests}")
}
