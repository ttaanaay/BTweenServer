package com.btweeu.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: Long,
    val type: String,
    val actorId: Long,
    val actorUsername: String,
    val actorDisplayName: String,
    val quoteId: Long?,
    val quoteTextPreview: String?,
    val isRead: Boolean,
    val createdAt: String
)

@Serializable
data class UnreadCountResponse(val count: Long)
