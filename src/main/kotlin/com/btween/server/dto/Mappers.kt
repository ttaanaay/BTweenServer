package com.btween.server.dto

import com.btween.server.data.repository.UserRepository
import com.btween.server.domain.Quote
import com.btween.server.domain.User
import java.time.format.DateTimeFormatter

fun User.toResponse(
    userRepository: UserRepository,
    viewerId: Long?
): UserResponse = UserResponse(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    followerCount = userRepository.followerCount(id),
    followingCount = userRepository.followingCount(id),
    isFollowedByMe = viewerId != null && viewerId != id && userRepository.isFollowing(viewerId, id)
)

fun Quote.toResponse(owner: UserResponse, isLikedByMe: Boolean): QuoteResponse = QuoteResponse(
    id = id,
    text = text,
    sourceTitle = sourceTitle,
    sourceType = sourceType,
    speaker = speaker,
    author = author,
    category = category,
    tags = tags,
    visibility = visibility,
    likeCount = likeCount,
    isLikedByMe = isLikedByMe,
    owner = owner,
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt),
    updatedAt = DateTimeFormatter.ISO_INSTANT.format(updatedAt)
)
