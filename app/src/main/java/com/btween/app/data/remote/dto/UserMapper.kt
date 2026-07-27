package com.btween.app.data.remote.dto

import com.btween.app.domain.model.User

fun UserResponseDto.toDomain(): User = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    followerCount = followerCount,
    followingCount = followingCount,
    isFollowedByMe = isFollowedByMe
)
