package com.btweeu.server.dto

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
    val emailVerified: Boolean = true,
    // null = signed up with email/password and has a password to confirm with; non-null
    // (e.g. "GOOGLE") = signed in via a provider and has no password at all.
    val authProvider: String? = null
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
