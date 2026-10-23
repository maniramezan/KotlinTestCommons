package io.github.maniramezan.testcommons

/**
 * Builds stable, cross-platform UI automation identifiers such as
 * `myapp.home.feed.video_card.intro%20clip`.
 *
 * Identifiers have the shape `<namespace>.<screen>.<region>.<element>[.<key>]`. Every segment
 * except the key must be lowercase snake_case; the optional key, such as a content ID, is
 * percent-encoded with only RFC 3986 unreserved characters left as-is, so iOS and Android
 * produce identical strings. Keep an app's catalog of identifiers in the app and build each
 * one with [make]; use them for test tags, not for spoken accessibility text.
 *
 * ```kotlin
 * val ids = AutomationId("myapp")
 * Modifier.testTag(ids.make("home", "feed", "video_card", key = video.id))
 * ```
 *
 * @property namespace The app-wide first segment.
 */
public class AutomationId(public val namespace: String) {
    init {
        require(isSegment(namespace)) { "namespace must be lowercase snake_case" }
    }

    /**
     * Returns the identifier for one element.
     *
     * @param screen The screen segment, in lowercase snake_case.
     * @param region The region segment, in lowercase snake_case.
     * @param element The element segment, in lowercase snake_case.
     * @param key An optional instance key; it is percent-encoded.
     * @return The dot-separated identifier.
     * @throws IllegalArgumentException when a segment is not lowercase snake_case.
     */
    public fun make(screen: String, region: String, element: String, key: String? = null): String {
        require(isSegment(screen)) { "screen must be lowercase snake_case" }
        require(isSegment(region)) { "region must be lowercase snake_case" }
        require(isSegment(element)) { "element must be lowercase snake_case" }
        val segments = listOfNotNull(namespace, screen, region, element, key?.let(::encodeKey))
        return segments.joinToString(".")
    }

    public companion object {
        private val SEGMENT = Regex("^[a-z][a-z0-9_]*$")
        private const val HEX = "0123456789ABCDEF"

        /**
         * Returns whether [value] is a valid lowercase snake_case segment.
         *
         * @param value The candidate segment.
         * @return `true` when it starts with a letter and contains only `a-z`, `0-9`, and `_`.
         */
        public fun isSegment(value: String): Boolean = SEGMENT.matches(value)

        /**
         * Percent-encodes [key] as UTF-8, leaving `A-Z a-z 0-9 - . _ ~` unchanged.
         *
         * @param key The raw key.
         * @return The encoded key, with uppercase hex digits.
         */
        public fun encodeKey(key: String): String =
            buildString {
                for (byte in key.encodeToByteArray()) {
                    val code = byte.toInt() and 0xFF
                    val char = code.toChar()
                    if (char.isUnreserved()) {
                        append(char)
                    } else {
                        append('%')
                        append(HEX[code shr 4])
                        append(HEX[code and 0x0F])
                    }
                }
            }

        private fun Char.isUnreserved(): Boolean =
            this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '.' ||
                this == '_' || this == '~'
    }
}
