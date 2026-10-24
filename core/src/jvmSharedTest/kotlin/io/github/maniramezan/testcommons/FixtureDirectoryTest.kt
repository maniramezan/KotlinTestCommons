package io.github.maniramezan.testcommons

import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FixtureDirectoryTest {
    @Test
    fun readsTextAndBytesBeneathTheRoot() {
        val directory = TemporaryDirectory()
        try {
            directory.write(" text \n", "nested/a.txt")
            val fixtures = FixtureDirectory(directory.file)
            assertEquals(" text \n", fixtures.text("nested/a.txt"))
            assertContentEquals(" text \n".encodeToByteArray(), fixtures.bytes("nested/a.txt"))
            assertFailsWith<FileNotFoundException> { fixtures.bytes("missing.txt") }
            assertFailsWith<FileNotFoundException> { fixtures.bytes("nested") }
            assertFailsWith<IllegalArgumentException> { fixtures.bytes("../escape.txt") }
            directory.write(byteArrayOf(0xC3.toByte()), "invalid.txt")
            assertFailsWith<CharacterCodingException> { fixtures.text("invalid.txt") }
        } finally {
            directory.remove()
        }
    }

    @Test
    fun readsFromTheOwningClassPath() {
        val fixtures = FixtureDirectory.fromResources(FixtureDirectoryTest::class.java, "fixtures")
        assertEquals("hello\n", fixtures.text("hello.txt"))
        assertFailsWith<FileNotFoundException> {
            FixtureDirectory.fromResources(FixtureDirectoryTest::class.java, "does-not-exist")
        }
        assertFailsWith<FileNotFoundException> {
            FixtureDirectory.fromResources(Test::class.java, "org/junit")
        }
    }

    @Test
    fun rejectsTheClassPathRoot() {
        assertFailsWith<IllegalArgumentException> {
            FixtureDirectory.fromResources(FixtureDirectoryTest::class.java, "")
        }
        assertFailsWith<IllegalArgumentException> {
            FixtureDirectory.fromResources(FixtureDirectoryTest::class.java, "/")
        }
    }
}
