package com.btween.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowedByMe: Boolean = false,
    val emailVerified: Boolean = true
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null
)

@Serializable
data class TopContributorResponse(
    val user: UserResponse,
    val quoteCount: Int
)
