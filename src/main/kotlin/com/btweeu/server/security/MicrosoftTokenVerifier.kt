package com.btweeu.server.security

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Serializable

/**
 * Same approach as [FacebookTokenVerifier]: Microsoft access tokens are verified by calling
 * Microsoft Graph's `/me` endpoint - an invalid/expired token makes this call fail, which
 * verifies the token and fetches the profile together.
 */
class MicrosoftTokenVerifier(private val httpClient: HttpClient) {

    suspend fun verify(accessToken: String): OAuthProfile? = runCatching {
        val response: MicrosoftProfileResponse = httpClient.get("https://graph.microsoft.com/v1.0/me") {
            header("Authorization", "Bearer $accessToken")
        }.body()

        OAuthProfile(
            providerUserId = response.id,
            email = response.mail ?: response.userPrincipalName,
            name = response.displayName
        )
    }.getOrNull()
}

@Serializable
private data class MicrosoftProfileResponse(
    val id: String,
    val displayName: String? = null,
    val mail: String? = null,
    val userPrincipalName: String? = null
)
