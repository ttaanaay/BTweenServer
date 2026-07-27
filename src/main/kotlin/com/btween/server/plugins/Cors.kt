package com.btween.server.plugins

import com.btween.server.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(config: AppConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)

        val host = config.allowedCorsHost
        if (host != null) {
            allowHost(host, schemes = listOf("https", "http"))
        } else {
            anyHost() // fine for a mobile-only client with no cookies; tighten if a web client is added later
        }
    }
}
