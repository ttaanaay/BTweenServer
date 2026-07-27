package com.btween.server

import com.btween.server.config.AppConfig
import com.btween.server.config.DatabaseFactory
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.plugins.configureCors
import com.btween.server.plugins.configureSecurity
import com.btween.server.plugins.configureSerialization
import com.btween.server.plugins.configureStatusPages
import com.btween.server.routes.authRoutes
import com.btween.server.routes.quoteRoutes
import com.btween.server.routes.userRoutes
import com.btween.server.security.JwtService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
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
    val jwtService = JwtService(config)

    install(CallLogging)
    configureSerialization()
    configureStatusPages()
    configureCors(config)
    configureSecurity(config)

    routing {
        // Health check - used by Render (and any uptime monitor) to confirm the service is alive.
        get("/") {
            call.respondText("BTween server is running")
        }

        authRoutes(userRepository, jwtService)
        userRoutes(userRepository, quoteRepository)
        quoteRoutes(quoteRepository, userRepository)
    }
}
