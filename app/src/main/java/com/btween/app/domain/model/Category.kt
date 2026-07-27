package com.btween.app.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val quoteCount: Int = 0
)

/**
 * Seed categories created the first time the database is opened. Users can rename, recolor,
 * or delete these like any other category (deleting a default category does not delete the
 * quotes assigned to it — see [com.btween.app.domain.usecase.category.DeleteCategoryUseCase]).
 */
object DefaultCategories {
    val seed: List<Pair<String, String>> = listOf(
        "Motivation" to "#E8A94C",
        "Success" to "#4CAF7D",
        "Love" to "#E85D8A",
        "Friendship" to "#5DA8E8",
        "Philosophy" to "#8E7CC3",
        "Life" to "#6FBF73",
        "Business" to "#4C7CE8",
        "Family" to "#E87D4C",
        "Funny" to "#E8C64C"
    )
}
