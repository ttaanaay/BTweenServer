package com.btween.app.domain.model

data class Quote(
    val id: Long = 0L,
    val text: String,
    val sourceTitle: String,
    val sourceType: SourceType,
    val speaker: String,
    val author: String? = null,
    val category: Category? = null,
    val tags: List<String> = emptyList(),
    val note: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastViewedAt: Long? = null
)
