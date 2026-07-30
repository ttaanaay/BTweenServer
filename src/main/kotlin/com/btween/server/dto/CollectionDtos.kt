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
    val createdAt: String
)

@Serializable
data class CollectionDetailResponse(
    val id: Long,
    val name: String,
    val quotes: List<QuoteResponse>,
    val createdAt: String
)
