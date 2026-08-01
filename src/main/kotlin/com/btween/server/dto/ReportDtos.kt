package com.btween.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReportRequest(
    val targetType: String,
    val targetId: Long,
    val reason: String,
    val details: String? = null
)

@Serializable
data class ReportResponse(
    val id: Long,
    val targetType: String,
    val targetId: Long,
    val reason: String,
    val details: String?,
    val status: String,
    val reporterUsername: String,
    // A short preview of the actual reported content, so an admin doesn't have to go dig it
    // up separately - the quote text, the comment text, or the reported user's @handle.
    // Null if the content was deleted since the report was filed.
    val targetPreview: String?,
    val createdAt: String
)
