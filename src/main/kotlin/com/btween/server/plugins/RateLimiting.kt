package com.btween.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

/**
 * Two buckets, keyed by client IP:
 * - [AUTH_RATE_LIMIT]: 5 requests/minute - applied to /auth/register, /auth/login,
 *   /auth/refresh. Tight enough to make password brute-forcing impractical, loose enough
 *   that a real user mistyping their password a couple of times never notices it.
 * - [API_RATE_LIMIT]: 100 requests/minute - applied to everything else, as a basic guard
 *   against a client (buggy or malicious) hammering the feed/like/follow endpoints.
 */
val AUTH_RATE_LIMIT = RateLimitName("auth")
val API_RATE_LIMIT = RateLimitName("api")

fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(AUTH_RATE_LIMIT) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(API_RATE_LIMIT) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
