package io.github.maniramezan.testcommons

/**
 * A payload-free error for exercising failure paths without a domain dependency.
 *
 * All instances compare equal, so a test can assert that a failure surfaced unchanged:
 *
 * ```kotlin
 * val result = runCatching { repository.load() }
 * assertEquals(TestError(), result.exceptionOrNull())
 * ```
 */
public class TestError : Exception("test error") {
    override fun equals(other: Any?): Boolean = other is TestError

    override fun hashCode(): Int = TestError::class.hashCode()
}
