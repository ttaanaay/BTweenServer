package com.btween.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuoteRequestDto(
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
data class QuoteResponseDto(
    val id: Long,
    val text: String,
    val sourceTitle: String,
    val sourceType: String,
    val speaker: String,
    val author: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val visibility: String,
    val likeCount: Int,
    val isLikedByMe: Boolean = false,
    val owner: UserResponseDto,
    val createdAt: String,
    val updatedAt: String
)
