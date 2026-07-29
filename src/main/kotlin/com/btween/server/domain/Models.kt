package com.btween.server.domain

import java.time.Instant

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val passwordHash: String?,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val isAdmin: Boolean,
    val isBanned: Boolean,
    val autoApprove: Boolean?,
    val authProvider: String?,
    val providerUserId: String?,
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
    val imageUrl: String?,
    val visibility: String,
    val status: String,
    val likeCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AppSettingsData(
    val defaultAutoApprove: Boolean
)

data class Notification(
    val id: Long,
    val recipientUserId: Long,
    val actorUserId: Long,
    val type: String,
    val quoteId: Long?,
    val isRead: Boolean,
    val createdAt: Instant
)

data class Comment(
    val id: Long,
    val quoteId: Long,
    val userId: Long,
    val text: String,
    val createdAt: Instant
)
