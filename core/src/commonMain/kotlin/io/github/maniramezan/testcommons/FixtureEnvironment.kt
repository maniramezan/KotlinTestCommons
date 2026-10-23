package io.github.maniramezan.testcommons

/**
 * Builds an isolated environment map for injection or a child process.
 *
 * Never reads or mutates the current process's environment. `null` overrides remove keys.
 *
 * @param base The environment to copy; defaults to an empty environment.
 * @param overrides Variables to replace, or remove when the value is `null`.
 * @return The independent environment map.
 */
public fun fixtureEnvironment(
    base: Map<String, String> = emptyMap(),
    overrides: Map<String, String?>,
): Map<String, String> {
    val result = base.toMutableMap()
    for ((key, value) in overrides) {
        if (value == null) result.remove(key) else result[key] = value
    }
    return result
}
