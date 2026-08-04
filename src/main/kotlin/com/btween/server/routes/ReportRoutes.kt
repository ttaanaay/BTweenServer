package com.btween.server.routes

import com.btween.server.data.repository.ReportRepository
import com.btween.server.dto.ReportRequest
import com.btween.server.exception.ValidationException
import com.btween.server.plugins.AUTH_JWT
import com.btween.server.plugins.WRITE_RATE_LIMIT
import com.btween.server.plugins.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val VALID_TARGET_TYPES = setOf("QUOTE", "USER", "COMMENT")
private val VALID_REASONS = setOf("SPAM", "HARASSMENT", "INAPPROPRIATE", "MISINFORMATION", "OTHER")

fun Route.reportRoutes(reportRepository: ReportRepository) {
    route("/reports") {
        authenticate(AUTH_JWT) {
            rateLimit(WRITE_RATE_LIMIT) {
                post {
                    val userId = call.requireUserId()
                    val request = call.receive<ReportRequest>()

                    if (request.targetType.uppercase() !in VALID_TARGET_TYPES) {
                        throw ValidationException("Invalid target type")
                    }
                    if (request.reason.uppercase() !in VALID_REASONS) {
                        throw ValidationException("Invalid reason")
                    }

                    reportRepository.create(
                        reporterId = userId,
                        targetType = request.targetType.uppercase(),
                        targetId = request.targetId,
                        reason = request.reason.uppercase(),
                        details = request.details?.trim()?.takeIf { it.isNotEmpty() }
                    )
                    call.respond(HttpStatusCode.Created)
                }
            }
        }
    }
}
