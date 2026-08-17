package com.btweeu.server.plugins

import com.btweeu.server.config.AppConfig
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

        if (config.allowedCorsHosts.isNotEmpty()) {
            config.allowedCorsHosts.forEach { host ->
                allowHost(host, schemes = listOf("https", "http"))
            }
        } else {
            anyHost() // fine for a mobile-only client with no cookies; tighten if a web client is added later
        }
    }
}
