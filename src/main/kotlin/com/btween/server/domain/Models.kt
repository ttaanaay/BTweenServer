package com.btween.server.domain

import java.time.Instant

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val createdAt: Instant
)

data class Quote(
    val id: Long,
    val ownerId: Long,
    val text: String,
    val sourceTitle: String,
    val sourceType: String,
    val speaker: String,
    val author: String?,
    val category: String?,
    val tags: List<String>,
    val visibility: String,
    val likeCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
