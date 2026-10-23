package io.github.maniramezan.testcommons

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TemporaryDirectoryTest {
    @Test
    fun createsUniqueDirectoriesAndWritesNestedFiles() {
        val first = TemporaryDirectory(prefix = "Import")
        val second = TemporaryDirectory()
        try {
            assertNotEquals(first.file, second.file)
            assertTrue(first.file.name.startsWith("Import-"))
            val file = first.write("{}", "fixtures/empty.json")
            assertEquals("{}", file.readText())
            assertEquals(File(first.file, "fixtures/empty.json").canonicalFile, file)
        } finally {
            first.remove()
            second.remove()
        }
        assertFalse(first.file.exists())
        first.remove()
    }

    @Test
    fun rejectsEscapingPathsAndInvalidPrefixes() {
        val directory = TemporaryDirectory()
        try {
            for (name in listOf("", "../outside", "/absolute", "a/../../outside")) {
                assertFailsWith<IllegalArgumentException> { directory.write(byteArrayOf(), name) }
            }
        } finally {
            directory.remove()
        }
        for (prefix in listOf("", "a/b", ".", "..")) {
            assertFailsWith<IllegalArgumentException> { TemporaryDirectory(prefix = prefix) }
        }
    }

    @Test
    fun createsInsideTheGivenParentAndNeverReusesADirectory() {
        val parent = TemporaryDirectory()
        try {
            val child = TemporaryDirectory(parent = parent.file)
            assertEquals(parent.file, child.file.parentFile)
            File(parent.file, "missing/child").let { missing ->
                assertFailsWith<java.nio.file.NoSuchFileException> { TemporaryDirectory(parent = missing) }
            }
        } finally {
            parent.remove()
        }
    }
}
