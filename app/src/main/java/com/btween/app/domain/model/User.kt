package com.btween.app.domain.model

data class User(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowedByMe: Boolean = false
)
