package com.btween.app.data.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Stores the `tags` field of [com.btween.app.data.local.entity.QuoteEntity] as a compact
 * JSON array string, since Room has no native support for List<String> columns.
 */
class TagsConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTags(tags: List<String>): String = json.encodeToString(tags)

    @TypeConverter
    fun toTags(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }
}
