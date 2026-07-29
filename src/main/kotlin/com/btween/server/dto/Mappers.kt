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
    imageUrl = imageUrl,
    visibility = visibility,
    status = status,
    likeCount = likeCount,
    isLikedByMe = isLikedByMe,
    owner = owner,
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt),
    updatedAt = DateTimeFormatter.ISO_INSTANT.format(updatedAt)
)

fun User.toAdminResponse(userRepository: UserRepository): AdminUserResponse = AdminUserResponse(
    id = id,
    username = username,
    email = email,
    displayName = displayName,
    isAdmin = isAdmin,
    isBanned = isBanned,
    autoApprove = autoApprove,
    followerCount = userRepository.followerCount(id),
    followingCount = userRepository.followingCount(id),
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt)
)

fun Quote.toAdminResponse(ownerUsername: String): AdminQuoteResponse = AdminQuoteResponse(
    id = id,
    text = text,
    sourceTitle = sourceTitle,
    sourceType = sourceType,
    speaker = speaker,
    visibility = visibility,
    status = status,
    likeCount = likeCount,
    ownerId = ownerId,
    ownerUsername = ownerUsername,
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt)
)
