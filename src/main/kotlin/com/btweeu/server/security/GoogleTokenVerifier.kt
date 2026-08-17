package com.btweeu.server.security

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

/**
 * Verifies the ID token the Android app gets back from Google Sign-In. Verification is
 * fully offline/cryptographic (checks the token's signature against Google's published
 * public keys, its audience against our own OAuth client ID, and its expiry) - no network
 * call to Google is needed per sign-in after the verifier's key cache is warmed.
 */
class GoogleTokenVerifier(clientId: String) {

    private val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(listOf(clientId))
        .build()

    fun verify(idTokenString: String): OAuthProfile? = runCatching {
        val idToken = verifier.verify(idTokenString) ?: return null
        val payload = idToken.payload
        OAuthProfile(
            providerUserId = payload.subject,
            email = payload.email,
            name = payload["name"] as? String
        )
    }.getOrNull()
}
