package com.btween.app.data.remote

import com.btween.app.data.remote.dto.AuthResponseDto
import com.btween.app.data.remote.dto.RefreshRequestDto
import com.btween.app.di.BaseUrl
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * When any authenticated request comes back 401, this refreshes the access token using the
 * stored refresh token and retries the original request once with the new token.
 *
 * Deliberately uses a bare [OkHttpClient] (no [AuthInterceptor]/self-reference) to perform the
 * refresh call directly, rather than going through Retrofit's AuthApi - that would create a
 * circular dependency (the authenticated OkHttpClient would depend on an API that itself
 * depends on that same client).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @BaseUrl private val baseUrl: String
) : Authenticator {

    private val plainClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once for this request - give up rather than looping forever.
        if (response.request.header("X-Retry-After-Refresh") != null) {
            tokenManager.clearSession()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearSession()
            return null
        }

        val newAccessToken = synchronized(this) {
            // Another thread may have already refreshed while we were waiting on the lock.
            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentToken != null && currentToken != requestToken) {
                currentToken
            } else {
                refreshAccessToken(refreshToken)
            }
        } ?: run {
            tokenManager.clearSession()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .header("X-Retry-After-Refresh", "true")
            .build()
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val body = json.encodeToString(RefreshRequestDto.serializer(), RefreshRequestDto(refreshToken))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${baseUrl}auth/refresh")
                .post(body)
                .build()

            plainClient.newCall(request).execute().use { httpResponse ->
                if (!httpResponse.isSuccessful) return null
                val responseBody = httpResponse.body?.string() ?: return null
                val auth = json.decodeFromString(AuthResponseDto.serializer(), responseBody)
                tokenManager.saveSession(auth.accessToken, auth.refreshToken, auth.user.id)
                auth.accessToken
            }
        } catch (e: Exception) {
            null
        }
    }
}
