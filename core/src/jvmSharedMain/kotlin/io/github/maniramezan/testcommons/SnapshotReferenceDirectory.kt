package io.github.maniramezan.testcommons

import java.io.File

/**
 * Finds an existing snapshot reference directory for a test class without choosing a
 * snapshot library or recording policy.
 *
 * For a test class `com.example.HomeScreenTest`, candidates are
 * `<root>/<directoryName>/HomeScreenTest/` for each root, in order. Only existing directories
 * match. Pass the result's path to Roborazzi, Paparazzi, or another library, and fall back to
 * its default location when this returns `null`.
 */
public object SnapshotReferenceDirectory {
    /** The default reference folder name. */
    public const val DEFAULT_DIRECTORY_NAME: String = "__snapshots__"

    /**
     * Returns the first existing reference directory for [testClass].
     *
     * @param testClass The test class whose simple name names the folder.
     * @param roots Directories to search, in order, such as `File("src/test")`.
     * @param directoryName The folder under each root that holds per-test folders.
     * @return The directory, or `null` when none exists.
     */
    public fun resolve(
        testClass: Class<*>,
        roots: List<File>,
        directoryName: String = DEFAULT_DIRECTORY_NAME,
    ): File? = resolve(testClass.simpleName, roots, directoryName)

    /**
     * Returns the first existing reference directory named [testName].
     *
     * @param testName The per-test folder name.
     * @param roots Directories to search, in order.
     * @param directoryName The folder under each root that holds per-test folders.
     * @return The directory, or `null` when none exists.
     */
    public fun resolve(
        testName: String,
        roots: List<File>,
        directoryName: String = DEFAULT_DIRECTORY_NAME,
    ): File? = roots.map { File(File(it, directoryName), testName) }.firstOrNull { it.isDirectory }
}
