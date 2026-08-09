package com.btween.server.routes

import com.btween.server.data.repository.AppSettingsRepository
import com.btween.server.data.repository.DeviceTokenRepository
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.exception.NotFoundException
import com.btween.server.exception.UnauthorizedException
import com.btween.server.push.PushNotificationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class SendDailyQuoteResponse(val sentTo: Int, val quoteText: String)

/**
 * [cronSecret] is null if FIREBASE_SERVICE_ACCOUNT_JSON or DAILY_QUOTE_CRON_SECRET aren't
 * configured yet - the route still exists in that case, but always responds 503, so the
 * external cron job's calls fail loudly (in its own logs) rather than silently no-op-ing.
 */
fun Route.cronRoutes(
    quoteRepository: QuoteRepository,
    deviceTokenRepository: DeviceTokenRepository,
    pushNotificationService: PushNotificationService?,
    cronSecret: String?,
    appSettingsRepository: AppSettingsRepository
) {
    route("/cron") {
        post("/send-daily-quote") {
            if (cronSecret == null || pushNotificationService == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, "Push notifications aren't configured on this server yet")
                return@post
            }

            val providedSecret = call.request.header("X-Cron-Secret")
            if (providedSecret != cronSecret) throw UnauthorizedException("Invalid cron secret")

            val quote = resolveDailyQuote(quoteRepository, appSettingsRepository)
                ?: throw NotFoundException("No public quotes to feature yet")
            val tokens = deviceTokenRepository.getAllTokens()

            val body = "\u201C${quote.text.take(120)}\u201D \u2014 ${quote.speaker}"
            val invalidTokens = if (tokens.isNotEmpty()) {
                pushNotificationService.sendToTokens(tokens, "Today's quote", body)
            } else {
                emptyList()
            }
            invalidTokens.forEach { deviceTokenRepository.removeInvalidToken(it) }

            call.respond(SendDailyQuoteResponse(sentTo = tokens.size - invalidTokens.size, quoteText = quote.text))
        }
    }
}
