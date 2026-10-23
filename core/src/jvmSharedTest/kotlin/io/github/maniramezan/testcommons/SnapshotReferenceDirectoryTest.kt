package io.github.maniramezan.testcommons

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SnapshotReferenceDirectoryTest {
    @Test
    fun returnsTheFirstExistingDirectoryInRootOrder() {
        val first = TemporaryDirectory()
        val second = TemporaryDirectory()
        try {
            val name = "SnapshotReferenceDirectoryTest"
            assertNull(SnapshotReferenceDirectory.resolve(this::class.java, listOf(first.file, second.file)))
            first.write("not a directory", "__snapshots__/$name")
            second.write("", "__snapshots__/$name/home.png")
            assertEquals(
                File(second.file, "__snapshots__/$name"),
                SnapshotReferenceDirectory.resolve(this::class.java, listOf(first.file, second.file)),
            )
            second.write("", "refs/Other/home.png")
            assertEquals(
                File(second.file, "refs/Other"),
                SnapshotReferenceDirectory.resolve("Other", listOf(first.file, second.file), directoryName = "refs"),
            )
        } finally {
            first.remove()
            second.remove()
        }
    }
}
