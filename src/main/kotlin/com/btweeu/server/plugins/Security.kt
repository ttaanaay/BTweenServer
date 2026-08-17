package com.btweeu.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.btweeu.server.config.AppConfig
import com.btweeu.server.dto.ErrorResponse
import com.btweeu.server.exception.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond

const val AUTH_JWT = "auth-jwt"

fun Application.configureSecurity(config: AppConfig) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = config.jwtIssuer
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwtSecret))
                    .withIssuer(config.jwtIssuer)
                    .build()
            )
            validate { credential ->
                val type = credential.payload.getClaim("type").asString()
                val userId = credential.payload.getClaim("userId").asLong()
                if (type == "access" && userId != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Token is missing, invalid, or expired"))
            }
        }
    }
}

/**
 * Extracts the authenticated user's id from the validated JWT principal. Only call this
 * from inside an `authenticate(AUTH_JWT) { ... }` block, where a principal is guaranteed.
 */
fun ApplicationCall.requireUserId(): Long =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
        ?: throw UnauthorizedException("Token is missing, invalid, or expired")
