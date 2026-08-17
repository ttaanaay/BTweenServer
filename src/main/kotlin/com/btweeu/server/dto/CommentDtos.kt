package com.btweeu.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(
    val text: String
)

@Serializable
data class CommentResponse(
    val id: Long,
    val quoteId: Long,
    val text: String,
    val author: UserResponse,
    val isEdited: Boolean = false,
    val createdAt: String
)
