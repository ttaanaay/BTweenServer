package com.btween.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.btween.server.config.AppConfig
import java.util.Date

/**
 * Issues two token types:
 * - Access token: short-lived (default 60 min), sent with every API request.
 * - Refresh token: long-lived (default 30 days), used only to mint new access tokens
 *   via POST /auth/refresh, so a stolen access token has a small blast radius.
 *
 * Both are plain JWTs signed with the same HMAC secret; they're told apart by the
 * "type" claim, which every protected route must check (see [Security.kt]'s validator).
 */
class JwtService(private val config: AppConfig) {

    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun generateAccessToken(userId: Long): String = JWT.create()
        .withIssuer(config.jwtIssuer)
        .withClaim("userId", userId)
        .withClaim("type", "access")
        .withExpiresAt(Date(System.currentTimeMillis() + config.jwtAccessTokenExpiryMinutes * 60_000))
        .sign(algorithm)

    fun generateRefreshToken(userId: Long): String = JWT.create()
        .withIssuer(config.jwtIssuer)
        .withClaim("userId", userId)
        .withClaim("type", "refresh")
        .withExpiresAt(Date(System.currentTimeMillis() + config.jwtRefreshTokenExpiryDays * 86_400_000))
        .sign(algorithm)

    fun verifyRefreshToken(token: String): Long? = runCatching {
        val decoded = JWT.require(algorithm)
            .withIssuer(config.jwtIssuer)
            .build()
            .verify(token)
        if (decoded.getClaim("type").asString() != "refresh") return null
        decoded.getClaim("userId").asLong()
    }.getOrNull()
}
