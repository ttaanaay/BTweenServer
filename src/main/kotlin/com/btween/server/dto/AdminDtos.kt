package com.btween.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String,
    val isAdmin: Boolean,
    val isBanned: Boolean,
    // null = follows the global default setting; true/false = explicit per-user override.
    val autoApprove: Boolean?,
    val followerCount: Int,
    val followingCount: Int,
    val createdAt: String
)

@Serializable
data class AdminQuoteResponse(
    val id: Long,
    val text: String,
    val sourceTitle: String,
    val sourceType: String,
    val speaker: String,
    val visibility: String,
    val status: String,
    val likeCount: Int,
    val ownerId: Long,
    val ownerUsername: String,
    val createdAt: String
)

@Serializable
data class SetBannedRequest(val banned: Boolean)

@Serializable
data class SetAutoApproveRequest(val autoApprove: Boolean?)

@Serializable
data class AppSettingsResponse(val defaultAutoApprove: Boolean)

@Serializable
data class UpdateAppSettingsRequest(val defaultAutoApprove: Boolean)

@Serializable
data class AdminStatsResponse(
    val totalUsers: Long,
    val totalQuotes: Long,
    val pendingQuotes: Long,
    val approvedQuotes: Long,
    val rejectedQuotes: Long
)
