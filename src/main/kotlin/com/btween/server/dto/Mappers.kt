package com.btween.server.dto

import com.btween.server.data.repository.UserRepository
import com.btween.server.domain.Comment
import com.btween.server.domain.Quote
import com.btween.server.domain.QuoteCollection
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
    isFollowedByMe = viewerId != null && viewerId != id && userRepository.isFollowing(viewerId, id),
    emailVerified = emailVerified
)

fun Quote.toResponse(owner: UserResponse, isLikedByMe: Boolean, commentCount: Int = 0): QuoteResponse = QuoteResponse(
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
    commentCount = commentCount,
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
    emailVerified = emailVerified,
    isLocked = lockedUntil != null && lockedUntil.isAfter(java.time.Instant.now()),
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
    imageUrl = imageUrl,
    ownerId = ownerId,
    ownerUsername = ownerUsername,
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt)
)

fun Comment.toResponse(author: UserResponse): CommentResponse = CommentResponse(
    id = id,
    quoteId = quoteId,
    text = text,
    author = author,
    isEdited = updatedAt != null,
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt)
)

fun QuoteCollection.toResponse(quoteCount: Int): CollectionResponse = CollectionResponse(
    id = id,
    name = name,
    quoteCount = quoteCount,
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt)
)
