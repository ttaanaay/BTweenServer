package com.btween.server.config

import java.net.URI

/**
 * All configuration comes from environment variables so the exact same Docker image
 * works locally, on Render, or anywhere else - only the env vars change between
 * environments. Nothing sensitive is hardcoded or committed.
 */
data class AppConfig(
    val port: Int,
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAccessTokenExpiryMinutes: Long,
    val jwtRefreshTokenExpiryDays: Long,
    val allowedCorsHost: String?
) {
    companion object {
        /**
         * Parses a standard `postgresql://user:password@host:port/database` connection
         * string - exactly the format Supabase (and Render/Heroku-style hosts) hand out -
         * into the pieces HikariCP/JDBC need.
         */
        fun load(): AppConfig {
            val rawDatabaseUrl = requireEnv("DATABASE_URL")
            val uri = URI(rawDatabaseUrl)
            val (user, password) = uri.userInfo.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val jdbcUrl = "jdbc:postgresql://${uri.host}:${if (uri.port == -1) 5432 else uri.port}${uri.path}?sslmode=require"

            return AppConfig(
                port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
                jdbcUrl = jdbcUrl,
                dbUser = user,
                dbPassword = password,
                jwtSecret = requireEnv("JWT_SECRET"),
                jwtIssuer = System.getenv("JWT_ISSUER") ?: "btween-server",
                jwtAccessTokenExpiryMinutes = System.getenv("JWT_ACCESS_EXPIRY_MINUTES")?.toLongOrNull() ?: 60L,
                jwtRefreshTokenExpiryDays = System.getenv("JWT_REFRESH_EXPIRY_DAYS")?.toLongOrNull() ?: 30L,
                allowedCorsHost = System.getenv("CORS_ALLOWED_HOST")
            )
        }

        private fun requireEnv(name: String): String =
            System.getenv(name) ?: error("Missing required environment variable: $name")
    }
}
