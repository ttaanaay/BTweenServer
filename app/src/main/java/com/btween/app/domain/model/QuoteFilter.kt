package com.btween.app.domain.model

data class QuoteFilter(
    val categoryId: Long? = null,
    val sourceType: SourceType? = null,
    val favoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST
)
