package com.btween.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class CollectionRequest(
    val name: String
)

@Serializable
data class AddItemRequest(
    val quoteId: Long
)

@Serializable
data class CollectionResponse(
    val id: Long,
    val name: String,
    val quoteCount: Int,
    // The most recently added item's attached photo, if it has one - used as the cover
    // thumbnail in the app's Instagram-Highlights-style collection row. Null if the
    // collection is empty or none of its quotes have a photo attached.
    val coverImageUrl: String?,
    val createdAt: String
)

@Serializable
data class CollectionDetailResponse(
    val id: Long,
    val name: String,
    val quotes: List<QuoteResponse>,
    val createdAt: String
)
