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
    val createdAt: String
)
