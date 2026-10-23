package io.github.maniramezan.testcommons

import java.io.File

/**
 * Resolves [name] beneath [root], or returns `null` when it is empty, absolute, or escapes
 * the root through `..` segments or symbolic links.
 */
internal fun containedFile(name: String, root: File): File? {
    if (name.isEmpty() || File(name).isAbsolute || name.startsWith("/")) return null
    val base = root.canonicalFile
    val file = File(base, name).canonicalFile
    return file.takeIf { it.path.startsWith(base.path + File.separator) }
}
