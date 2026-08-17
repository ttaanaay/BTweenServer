package com.btweeu.server.data.repository

/**
 * The fixed set of icon keys a category can use. Kept as plain string keys (not emoji) so
 * every platform renders an identical, consistent icon instead of relying on the device's
 * emoji font. Android maps each key to a Material Icon it already ships with; the web app and
 * admin panel each ship a matching small icon set keyed the same way.
 */
object IconCatalog {
    val KEYS = listOf(
        "sun", "heart", "star", "groups", "brain", "laugh", "book", "movie",
        "music", "coffee", "moon", "trophy", "flame", "leaf", "compass", "sparkle",
        "water", "waves", "mountain", "home", "handshake", "flight", "work", "money",
        "health", "sport", "palette", "peace", "idea", "gift", "growth", "shield"
    )
    const val DEFAULT = "label"

    fun isValid(key: String): Boolean = key in KEYS
}
