package com.btween.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Never attach a (possibly stale) access token to the auth endpoints themselves.
        if (original.url.encodedPath.startsWith("/auth/")) {
            return chain.proceed(original)
        }

        val token = tokenManager.getAccessToken() ?: return chain.proceed(original)
        val authorized = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }
}
