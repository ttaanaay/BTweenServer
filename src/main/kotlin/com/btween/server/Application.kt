package com.btween.server

import com.btween.server.config.AppConfig
import com.btween.server.config.DatabaseFactory
import com.btween.server.data.repository.AnalyticsRepository
import com.btween.server.data.repository.AppSettingsRepository
import com.btween.server.data.repository.CategoryRepository
import com.btween.server.data.repository.CollectionRepository
import com.btween.server.data.repository.CommentRepository
import com.btween.server.data.repository.DeviceTokenRepository
import com.btween.server.data.repository.EmailVerificationRepository
import com.btween.server.data.repository.NotificationRepository
import com.btween.server.data.repository.PasswordResetRepository
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.data.repository.RefreshTokenRepository
import com.btween.server.data.repository.ReportRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.email.ConsoleEmailSender
import com.btween.server.plugins.API_RATE_LIMIT
import com.btween.server.plugins.configureCors
import com.btween.server.plugins.configureRateLimiting
import com.btween.server.plugins.configureSecurity
import com.btween.server.plugins.configureSerialization
import com.btween.server.plugins.configureStatusPages
import com.btween.server.push.PushNotificationService
import com.btween.server.routes.adminRoutes
import com.btween.server.routes.authRoutes
import com.btween.server.routes.categoryRoutes
import com.btween.server.routes.collectionRoutes
import com.btween.server.routes.commentRoutes
import com.btween.server.routes.cronRoutes
import com.btween.server.routes.deviceRoutes
import com.btween.server.routes.notificationRoutes
import com.btween.server.routes.quoteRoutes
import com.btween.server.routes.reportRoutes
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
    val notificationRepository = NotificationRepository()
    val commentRepository = CommentRepository()
    val collectionRepository = CollectionRepository()
    val reportRepository = ReportRepository()
    val analyticsRepository = AnalyticsRepository()
    val categoryRepository = CategoryRepository()
    categoryRepository.seedDefaultsIfEmpty()
    val refreshTokenRepository = RefreshTokenRepository()
    val passwordResetRepository = PasswordResetRepository()
    val emailVerificationRepository = EmailVerificationRepository()
    val deviceTokenRepository = DeviceTokenRepository()
    val emailSender = ConsoleEmailSender()
    val jwtService = JwtService(config)

    // Nullable, same pattern as the Google Sign-In verifier: the server boots fine without
    // this configured, only the daily-quote push feature won't work until it is.
    val pushNotificationService = config.firebaseServiceAccountJson?.let {
        try {
            PushNotificationService(it)
        } catch (e: Exception) {
            println("=== Firebase push notification setup failed: ${e.message} ===")
            e.printStackTrace()
            null
        }
    }

    println(
        "Daily quote push notification config: " +
            "FIREBASE_SERVICE_ACCOUNT_JSON=${if (config.firebaseServiceAccountJson != null) "set" else "MISSING"}, " +
            "DAILY_QUOTE_CRON_SECRET=${if (config.dailyQuoteCronSecret != null) "set" else "MISSING"}, " +
            "pushNotificationService=${if (pushNotificationService != null) "initialized OK" else "FAILED or not configured"}"
    )

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
        authRoutes(
            userRepository,
            jwtService,
            googleVerifier,
            facebookVerifier,
            microsoftVerifier,
            passwordResetRepository,
            emailVerificationRepository,
            refreshTokenRepository,
            emailSender,
            config
        )

        rateLimit(API_RATE_LIMIT) {
            userRoutes(userRepository, quoteRepository, notificationRepository)
            quoteRoutes(quoteRepository, userRepository, appSettingsRepository, notificationRepository, commentRepository)
            adminRoutes(userRepository, quoteRepository, appSettingsRepository, notificationRepository, reportRepository, commentRepository, analyticsRepository, categoryRepository)
            categoryRoutes(categoryRepository)
            notificationRoutes(notificationRepository, userRepository, quoteRepository)
            commentRoutes(commentRepository, quoteRepository, userRepository, notificationRepository)
            collectionRoutes(collectionRepository, quoteRepository, userRepository)
            reportRoutes(reportRepository)
            deviceRoutes(deviceTokenRepository)
        }

        // Not JWT-authenticated (an external cron service can't do an interactive login) -
        // protected by its own shared-secret header check instead. See CronRoutes.kt.
        cronRoutes(quoteRepository, deviceTokenRepository, pushNotificationService, config.dailyQuoteCronSecret, appSettingsRepository)
    }
}
