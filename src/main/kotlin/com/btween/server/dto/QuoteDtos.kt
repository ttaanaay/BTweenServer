package com.btween.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuoteRequest(
    val text: String,
    val sourceTitle: String,
    val sourceType: String,
    val speaker: String,
    val author: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val visibility: String = "PUBLIC"
)

@Serializable
data class QuoteResponse(
    val id: Long,
    val text: String,
    val sourceTitle: String,
    val sourceType: String,
    val speaker: String,
    val author: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val visibility: String,
    val status: String,
    val likeCount: Int,
    val isLikedByMe: Boolean = false,
    val owner: UserResponse,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ErrorResponse(
    val message: String
)
