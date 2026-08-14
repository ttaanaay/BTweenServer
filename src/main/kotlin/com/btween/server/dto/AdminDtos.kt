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
    val emailVerified: Boolean,
    val isLocked: Boolean,
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
    val imageUrl: String?,
    val ownerId: Long,
    val ownerUsername: String,
    val createdAt: String
)

@Serializable
data class SetBannedRequest(val banned: Boolean)

@Serializable
data class SetAdminStatusRequest(val isAdmin: Boolean)

@Serializable
data class CreateCategoryRequest(val name: String)

@Serializable
data class CreateSourceTypeRequest(val name: String)

@Serializable
data class FlaggedUserResponse(
    val userId: Long,
    val username: String,
    val displayName: String,
    val reportCount: Int,
    val isBanned: Boolean
)

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

@Serializable
data class AdminCommentResponse(
    val id: Long,
    val quoteId: Long,
    val text: String,
    val createdAt: String
)

@Serializable
data class AdminUserDetailResponse(
    val user: AdminUserResponse,
    val recentQuotes: List<AdminQuoteResponse>,
    val recentComments: List<AdminCommentResponse>
)

@Serializable
data class AnalyticsPoint(
    val date: String,
    val newUsers: Int,
    val newQuotes: Int,
    val newLikes: Int,
    val newComments: Int,
    val activeUsers: Int
)

@Serializable
data class AnalyticsResponse(
    val points: List<AnalyticsPoint>
)
