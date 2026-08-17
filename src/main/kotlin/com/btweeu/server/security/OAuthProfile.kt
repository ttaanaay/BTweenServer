package com.btweeu.server.security

data class OAuthProfile(
    val providerUserId: String,
    val email: String?,
    val name: String?
)
