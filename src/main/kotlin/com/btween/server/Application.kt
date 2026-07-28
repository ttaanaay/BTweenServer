package com.btween.server

import com.btween.server.config.AppConfig
import com.btween.server.config.DatabaseFactory
import com.btween.server.data.repository.AppSettingsRepository
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.plugins.API_RATE_LIMIT
import com.btween.server.plugins.configureCors
import com.btween.server.plugins.configureRateLimiting
import com.btween.server.plugins.configureSecurity
import com.btween.server.plugins.configureSerialization
import com.btween.server.plugins.configureStatusPages
import com.btween.server.routes.adminRoutes
import com.btween.server.routes.authRoutes
import com.btween.server.routes.quoteRoutes
import com.btween.server.routes.userRoutes
import com.btween.server.security.FacebookTokenVerifier
import com.btween.server.security.GoogleTokenVerifier
import com.btween.server.security.JwtService
import com.btween.server.security.MicrosoftTokenVerifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    val config = AppConfig.load()
    DatabaseFactory.init(config)

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig) {
    val userRepository = UserRepository()
    val quoteRepository = QuoteRepository()
    val appSettingsRepository = AppSettingsRepository()
    val jwtService = JwtService(config)

    val oauthHttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }
    val googleVerifier = config.googleClientId?.let { GoogleTokenVerifier(it) }
    val facebookVerifier = FacebookTokenVerifier(oauthHttpClient)
    val microsoftVerifier = MicrosoftTokenVerifier(oauthHttpClient)

    configureSerialization()
    configureStatusPages()
    configureCors(config)
    configureSecurity(config)
    configureRateLimiting()

    routing {
        // Health check - used by Render (and any uptime monitor) to confirm the service is
        // alive. Deliberately outside any rate-limit bucket, since monitors may ping this
        // every few seconds.
        get("/") {
            call.respondText("BTween server is running")
        }

        // authRoutes has its own internal AUTH_RATE_LIMIT wrapping (see AuthRoutes.kt) -
        // tighter than the general API limit, since brute-forcing login is the main risk.
        authRoutes(userRepository, jwtService, googleVerifier, facebookVerifier, microsoftVerifier)

        rateLimit(API_RATE_LIMIT) {
            userRoutes(userRepository, quoteRepository)
            quoteRoutes(quoteRepository, userRepository, appSettingsRepository)
            adminRoutes(userRepository, quoteRepository, appSettingsRepository)
        }
    }
}
