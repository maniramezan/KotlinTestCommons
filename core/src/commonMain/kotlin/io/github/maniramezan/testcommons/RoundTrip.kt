package io.github.maniramezan.testcommons

/**
 * Encodes and decodes [value] with the caller's serialization policy.
 *
 * Compare the result with the original in the owning test:
 *
 * ```kotlin
 * val decoded = roundTrip(user, { json.encodeToString(it) }, { json.decodeFromString<User>(it) })
 * assertEquals(user, decoded)
 * ```
 *
 * @param value The fixture to round-trip.
 * @param encode Serializes a value, including caller-specific formats and naming.
 * @param decode The matching deserializer.
 * @return The decoded value.
 */
public inline fun <T, E> roundTrip(value: T, encode: (T) -> E, decode: (E) -> T): T = decode(encode(value))
