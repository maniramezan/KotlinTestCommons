package io.github.maniramezan.testcommons

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Reads fixtures beneath one explicit root, rejecting paths that escape it.
 *
 * ```kotlin
 * val fixtures = FixtureDirectory(File("src/test/fixtures"))
 * val json = fixtures.text("responses/videos.json")
 * ```
 *
 * @property root The directory containing the fixtures; it need not exist yet.
 */
public class FixtureDirectory(public val root: File) {
    /**
     * Loads bytes from a relative path beneath [root].
     *
     * @param name A relative file path; traversal outside the root is rejected.
     * @return The fixture contents.
     * @throws IllegalArgumentException for an empty, absolute, or escaping path.
     * @throws FileNotFoundException when the fixture does not exist.
     */
    public fun bytes(name: String): ByteArray {
        val file = requireNotNull(containedFile(name, root)) { "$name is not contained in ${root.path}" }
        if (!file.isFile) throw FileNotFoundException(file.path)
        return file.readBytes()
    }

    /**
     * Loads a UTF-8 text fixture, preserving whitespace.
     *
     * @param name A relative file path beneath [root].
     * @return The decoded text.
     * @throws IOException when the fixture cannot be read.
     */
    public fun text(name: String): String = bytes(name).decodeToString(throwOnInvalidSequence = true)

    public companion object {
        /**
         * Returns a reader for a directory on [owner]'s class path, such as `src/test/resources/fixtures`.
         *
         * Pass a class from the owning test source set so another module's resources with the
         * same name cannot be selected. Resources packed in a JAR are not supported. The class
         * path root itself is rejected: it resolves to the first entry, usually compiled classes
         * rather than resources, so keep fixtures in a named folder.
         *
         * @param owner A class loaded from the test resources' class path entry.
         * @param directory The nonempty resource directory, relative to the class path root.
         * @return The reader.
         * @throws IllegalArgumentException when [directory] names the class path root.
         * @throws FileNotFoundException when the directory is not on the class path as a folder.
         */
        public fun fromResources(owner: Class<*>, directory: String): FixtureDirectory {
            val path = directory.trim('/')
            require(path.isNotEmpty()) { "directory must name a resource folder, not the class path root" }
            val url = owner.classLoader?.getResource(path)
                ?: throw FileNotFoundException("Resource directory '$path' is not on the class path")
            if (url.protocol != "file") throw FileNotFoundException("Resource directory '$path' is not a folder: $url")
            return FixtureDirectory(File(url.toURI()))
        }
    }
}
