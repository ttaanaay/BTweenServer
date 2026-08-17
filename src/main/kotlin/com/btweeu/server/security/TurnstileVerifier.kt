package com.btweeu.server.security

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.serialization.Serializable

@Serializable
data class TurnstileVerifyResponse(val success: Boolean)

class TurnstileVerifier(private val httpClient: HttpClient) {

    private val secretKey: String? = System.getenv("TURNSTILE_SECRET_KEY")?.trim()?.takeIf { it.isNotEmpty() }

    /** True if a secret key is configured, meaning verification is actually enforced. */
    val isEnabled: Boolean get() = secretKey != null

    /**
     * Returns true if the token is valid, or if Turnstile isn't configured (not enforced).
     * Also returns true (fails open) if the verification call to Cloudflare itself errors -
     * an outage on Cloudflare's side shouldn't be able to take down registration entirely.
     */
    suspend fun verify(token: String?): Boolean {
        val secret = secretKey ?: return true
        if (token.isNullOrBlank()) return false

        return try {
            val response = httpClient.submitForm(
                url = "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                formParameters = Parameters.build {
                    append("secret", secret)
                    append("response", token)
                }
            )
            response.body<TurnstileVerifyResponse>().success
        } catch (e: Exception) {
            true
        }
    }
}
