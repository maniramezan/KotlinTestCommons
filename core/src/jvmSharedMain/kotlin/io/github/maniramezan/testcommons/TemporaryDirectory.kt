package io.github.maniramezan.testcommons

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.UUID

/**
 * An independently owned scratch directory with a unique name, so parallel tests never share state.
 *
 * Cleanup is explicit: call [remove] when the test finishes, for example in a `finally` block
 * or an `@After` method, so a directory can outlive a single call when needed.
 *
 * ```kotlin
 * val directory = TemporaryDirectory(prefix = "ImportTests")
 * try {
 *     val file = directory.write("{}".encodeToByteArray(), "fixtures/empty.json")
 *     importer.import(file)
 * } finally {
 *     directory.remove()
 * }
 * ```
 *
 * @param prefix An optional label placed before the unique name, such as a test name, to identify
 *   leftover directories. It must be nonempty and must not contain a path separator.
 * @param parent The directory to create it in; defaults to `java.io.tmpdir`.
 * @throws IllegalArgumentException for an unusable prefix.
 * @throws IOException when the directory cannot be created.
 */
public class TemporaryDirectory(
    prefix: String? = null,
    parent: File = File(checkNotNull(System.getProperty("java.io.tmpdir")) { "java.io.tmpdir is not set" }),
) {
    /** The unique directory. */
    public val file: File

    init {
        val unique = UUID.randomUUID().toString()
        val name = if (prefix == null) {
            unique
        } else {
            require(prefix.isNotEmpty() && '/' !in prefix && '\\' !in prefix && prefix != "." && prefix != "..") {
                "prefix must be a nonempty name without path separators"
            }
            "$prefix-$unique"
        }
        file = Files.createDirectory(parent.toPath().resolve(name)).toFile()
    }

    /**
     * Writes a file beneath this directory, creating parent folders.
     *
     * @param bytes The contents to write.
     * @param name A relative path; paths escaping the directory are rejected.
     * @return The new file.
     * @throws IllegalArgumentException for an empty, absolute, or escaping path.
     * @throws IOException when the file cannot be written.
     */
    public fun write(bytes: ByteArray, name: String): File {
        val target = requireNotNull(containedFile(name, file)) { "$name is not contained in ${file.path}" }
        target.parentFile?.mkdirs()
        target.writeBytes(bytes)
        return target
    }

    /**
     * Writes UTF-8 text beneath this directory, creating parent folders.
     *
     * @param text The contents to write.
     * @param name A relative path; paths escaping the directory are rejected.
     * @return The new file.
     */
    public fun write(text: String, name: String): File = write(text.encodeToByteArray(), name)

    /**
     * Removes the directory and its contents. Removing an already-removed directory is harmless.
     *
     * @throws IOException when part of the tree cannot be deleted.
     */
    public fun remove() {
        if (!file.exists()) return
        if (!file.deleteRecursively()) throw IOException("Could not delete ${file.path}")
    }
}
