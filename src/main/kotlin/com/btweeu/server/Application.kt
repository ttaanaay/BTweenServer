package com.btweeu.server

import com.btweeu.server.config.AppConfig
import com.btweeu.server.config.DatabaseFactory
import com.btweeu.server.data.repository.AnalyticsRepository
import com.btweeu.server.data.repository.AppSettingsRepository
import com.btweeu.server.data.repository.CategoryRepository
import com.btweeu.server.data.repository.SourceTypeRepository
import com.btweeu.server.data.repository.CollectionRepository
import com.btweeu.server.data.repository.CommentRepository
import com.btweeu.server.data.repository.DeviceTokenRepository
import com.btweeu.server.data.repository.EmailVerificationRepository
import com.btweeu.server.data.repository.NotificationRepository
import com.btweeu.server.data.repository.PasswordResetRepository
import com.btweeu.server.data.repository.QuoteRepository
import com.btweeu.server.data.repository.RefreshTokenRepository
import com.btweeu.server.data.repository.ReportRepository
import com.btweeu.server.data.repository.UserRepository
import com.btweeu.server.email.ConsoleEmailSender
import com.btweeu.server.email.ResendEmailSender
import com.btweeu.server.plugins.API_RATE_LIMIT
import com.btweeu.server.plugins.configureCors
import com.btweeu.server.plugins.configureRateLimiting
import com.btweeu.server.plugins.configureSecurity
import com.btweeu.server.plugins.configureSerialization
import com.btweeu.server.plugins.configureStatusPages
import com.btweeu.server.push.PushNotificationService
import com.btweeu.server.routes.adminRoutes
import com.btweeu.server.routes.authRoutes
import com.btweeu.server.routes.categoryRoutes
import com.btweeu.server.routes.maintenanceRoutes
import com.btweeu.server.routes.sourceTypeRoutes
import com.btweeu.server.routes.collectionRoutes
import com.btweeu.server.routes.commentRoutes
import com.btweeu.server.routes.cronRoutes
import com.btweeu.server.routes.deviceRoutes
import com.btweeu.server.routes.notificationRoutes
import com.btweeu.server.routes.quoteRoutes
import com.btweeu.server.routes.reportRoutes
import com.btweeu.server.routes.userRoutes
import com.btweeu.server.security.FacebookTokenVerifier
import com.btweeu.server.security.GoogleTokenVerifier
import com.btweeu.server.security.JwtService
import com.btweeu.server.security.MicrosoftTokenVerifier
import com.btweeu.server.security.TurnstileVerifier
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
    val sourceTypeRepository = SourceTypeRepository()
    sourceTypeRepository.seedDefaultsIfEmpty()
    val refreshTokenRepository = RefreshTokenRepository()
    val passwordResetRepository = PasswordResetRepository()
    val emailVerificationRepository = EmailVerificationRepository()
    val deviceTokenRepository = DeviceTokenRepository()
    val emailSender = if (config.resendApiKey != null && config.emailFromAddress != null) {
        println("Email: using Resend (from ${config.emailFromAddress})")
        ResendEmailSender(config.resendApiKey, config.emailFromAddress)
    } else {
        println("Email: RESEND_API_KEY/EMAIL_FROM_ADDRESS not set - falling back to console logging")
        ConsoleEmailSender()
    }
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
    val turnstileVerifier = TurnstileVerifier(oauthHttpClient)

    configureSerialization()
    configureStatusPages()
    println("CORS: allowed hosts = ${config.allowedCorsHosts.ifEmpty { listOf("(none configured - allowing any host)") }}")
    configureCors(config)
    configureSecurity(config)
    configureRateLimiting()

    routing {
        // Health check - used by Render (and any uptime monitor) to confirm the service is
        // alive. Deliberately outside any rate-limit bucket, since monitors may ping this
        // every few seconds.
        get("/") {
            call.respondText("Btweeu server is running")
        }

        // authRoutes has its own internal AUTH_RATE_LIMIT wrapping (see AuthRoutes.kt) -
        // tighter than the general API limit, since brute-forcing login is the main risk.
        authRoutes(
            userRepository,
            jwtService,
            googleVerifier,
            facebookVerifier,
            microsoftVerifier,
            turnstileVerifier,
            passwordResetRepository,
            emailVerificationRepository,
            refreshTokenRepository,
            emailSender,
            config
        )

        rateLimit(API_RATE_LIMIT) {
            userRoutes(userRepository, quoteRepository, notificationRepository)
            quoteRoutes(quoteRepository, userRepository, appSettingsRepository, notificationRepository, commentRepository)
            adminRoutes(userRepository, quoteRepository, appSettingsRepository, notificationRepository, reportRepository, commentRepository, analyticsRepository, categoryRepository, sourceTypeRepository)
            categoryRoutes(categoryRepository)
            sourceTypeRoutes(sourceTypeRepository)
            maintenanceRoutes(appSettingsRepository)
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
