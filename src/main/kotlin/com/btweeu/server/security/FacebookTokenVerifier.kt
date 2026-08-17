package com.btweeu.server.security

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/**
 * Facebook has no offline JWT-style verification for its access tokens - the standard
 * approach (and what Facebook's own docs recommend) is to call the Graph API's `/me`
 * endpoint with the token: if the token is invalid or expired, this call itself fails,
 * which both verifies the token and fetches the profile in one round trip.
 */
class FacebookTokenVerifier(private val httpClient: HttpClient) {

    suspend fun verify(accessToken: String): OAuthProfile? = runCatching {
        val response: FacebookProfileResponse = httpClient.get("https://graph.facebook.com/me") {
            parameter("fields", "id,name,email")
            parameter("access_token", accessToken)
        }.body()

        OAuthProfile(
            providerUserId = response.id,
            email = response.email,
            name = response.name
        )
    }.getOrNull()
}

@Serializable
private data class FacebookProfileResponse(
    val id: String,
    val name: String? = null,
    val email: String? = null
)
