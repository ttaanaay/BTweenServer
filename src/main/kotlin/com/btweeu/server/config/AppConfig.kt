package com.btweeu.server.config

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
    // Comma-separated list, e.g. "ttaanaay.github.io,btweeu.vercel.app" - lets both the
    // admin panel and the web app talk to the API without opening it up to any origin.
    val allowedCorsHosts: List<String>,
    // Only /auth/oauth/google needs this. Left null (rather than required) so the server
    // still boots normally before you've set up Google Sign-In - that one endpoint just
    // fails until it's configured, everything else works as before.
    val googleClientId: String?,
    // The full contents (not a file path) of the Firebase service account JSON key, so it
    // can be set directly as a Render env var without needing to commit or mount a file.
    // Only the daily-quote push notification feature needs this.
    val firebaseServiceAccountJson: String?,
    // A shared secret the external cron job (see docs) must send to trigger the daily
    // push - there's no interactive user to log in as, so this substitutes for JWT auth on
    // that one endpoint.
    val dailyQuoteCronSecret: String?,
    // Resend (https://resend.com) API key - only real-email-sending needs this. Left null
    // (rather than required) so the server still boots without it; ConsoleEmailSender is
    // used instead, logging codes to the server console.
    val resendApiKey: String?,
    // Must be on a domain verified in the Resend dashboard, e.g. "Btweeu <no-reply@yourdomain.com>".
    val emailFromAddress: String?,
    // Cloudflare Web Analytics (RUM) - powers the admin panel's visitor stats page. All three
    // are needed together; the feature is simply hidden if any is missing rather than
    // erroring, since it's optional and unrelated to core app function.
    val cloudflareApiToken: String?,
    val cloudflareAccountTag: String?,
    val cloudflareSiteTag: String?
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
                jwtIssuer = System.getenv("JWT_ISSUER") ?: "btweeu-server",
                jwtAccessTokenExpiryMinutes = System.getenv("JWT_ACCESS_EXPIRY_MINUTES")?.toLongOrNull() ?: 60L,
                jwtRefreshTokenExpiryDays = System.getenv("JWT_REFRESH_EXPIRY_DAYS")?.toLongOrNull() ?: 30L,
                allowedCorsHosts = System.getenv("CORS_ALLOWED_HOST")
                    ?.split(",")
                    ?.map { it.trim().removePrefix("https://").removePrefix("http://").trimEnd('/') }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList(),
                googleClientId = System.getenv("GOOGLE_CLIENT_ID"),
                firebaseServiceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON"),
                dailyQuoteCronSecret = System.getenv("DAILY_QUOTE_CRON_SECRET"),
                resendApiKey = System.getenv("RESEND_API_KEY"),
                emailFromAddress = System.getenv("EMAIL_FROM_ADDRESS"),
                cloudflareApiToken = System.getenv("CLOUDFLARE_API_TOKEN"),
                cloudflareAccountTag = System.getenv("CLOUDFLARE_ACCOUNT_TAG"),
                cloudflareSiteTag = System.getenv("CLOUDFLARE_SITE_TAG")
            )
        }

        private fun requireEnv(name: String): String =
            System.getenv(name) ?: error("Missing required environment variable: $name")
    }
}
