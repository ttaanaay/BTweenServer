package com.btween.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponseDto
)

@Serializable
data class UserResponseDto(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowedByMe: Boolean = false
)

@Serializable
data class UpdateProfileRequestDto(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null
)

@Serializable
data class ErrorResponseDto(
    val message: String
)
