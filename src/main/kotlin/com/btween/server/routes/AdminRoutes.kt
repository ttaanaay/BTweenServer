package com.btween.server.routes

import com.btween.server.data.repository.AnalyticsRepository
import com.btween.server.data.repository.AppSettingsRepository
import com.btween.server.data.repository.CommentRepository
import com.btween.server.data.repository.NotificationRepository
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.data.repository.ReportRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.domain.User
import com.btween.server.dto.AdminCommentResponse
import com.btween.server.dto.AdminStatsResponse
import com.btween.server.dto.AdminUserDetailResponse
import com.btween.server.dto.AnalyticsPoint
import com.btween.server.dto.AnalyticsResponse
import com.btween.server.dto.AppSettingsResponse
import com.btween.server.dto.ReportResponse
import com.btween.server.dto.SetAdminStatusRequest
import com.btween.server.dto.SetAutoApproveRequest
import com.btween.server.dto.SetBannedRequest
import com.btween.server.dto.UpdateAppSettingsRequest
import com.btween.server.dto.toAdminResponse
import com.btween.server.exception.ForbiddenException
import com.btween.server.exception.NotFoundException
import com.btween.server.exception.ValidationException
import com.btween.server.plugins.AUTH_JWT
import com.btween.server.plugins.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.format.DateTimeFormatter

/**
 * Verifies the caller is a signed-in admin, returning their [User]. Every admin route calls
 * this first - there's no separate "admin session" concept, just a regular JWT plus a check
 * of the isAdmin flag on the underlying account.
 */
private suspend fun ApplicationCall.requireAdmin(userRepository: UserRepository): User {
    val userId = requireUserId()
    val user = userRepository.findById(userId) ?: throw ForbiddenException("Admin access required")
    if (!user.isAdmin) throw ForbiddenException("Admin access required")
    return user
}

fun Route.adminRoutes(
    userRepository: UserRepository,
    quoteRepository: QuoteRepository,
    appSettingsRepository: AppSettingsRepository,
    notificationRepository: NotificationRepository,
    reportRepository: ReportRepository,
    commentRepository: CommentRepository,
    analyticsRepository: AnalyticsRepository
) {
    route("/admin") {
        authenticate(AUTH_JWT) {

            get("/stats") {
                call.requireAdmin(userRepository)
                call.respond(
                    AdminStatsResponse(
                        totalUsers = userRepository.countAll(),
                        totalQuotes = quoteRepository.countAll(),
                        pendingQuotes = quoteRepository.countByStatus("PENDING"),
                        approvedQuotes = quoteRepository.countByStatus("APPROVED"),
                        rejectedQuotes = quoteRepository.countByStatus("REJECTED")
                    )
                )
            }

            get("/users") {
                call.requireAdmin(userRepository)
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L
                val users = userRepository.getAllUsers(limit, offset)
                call.respond(users.map { it.toAdminResponse(userRepository) })
            }

            put("/users/{id}/ban") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid user id")
                val request = call.receive<SetBannedRequest>()
                val updated = userRepository.setBanned(id, request.banned)
                    ?: throw NotFoundException("User not found")
                call.respond(updated.toAdminResponse(userRepository))
            }

            put("/users/{id}/auto-approve") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid user id")
                val request = call.receive<SetAutoApproveRequest>()
                val updated = userRepository.setAutoApprove(id, request.autoApprove)
                    ?: throw NotFoundException("User not found")
                call.respond(updated.toAdminResponse(userRepository))
            }

            post("/users/{id}/unlock") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid user id")
                userRepository.findById(id) ?: throw NotFoundException("User not found")
                userRepository.resetFailedLogins(id)
                val updated = userRepository.findById(id)!!
                call.respond(updated.toAdminResponse(userRepository))
            }

            put("/users/{id}/admin-status") {
                val admin = call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid user id")
                if (id == admin.id) {
                    throw ValidationException("You can't change your own admin status")
                }
                val request = call.receive<SetAdminStatusRequest>()
                val updated = userRepository.setAdmin(id, request.isAdmin)
                    ?: throw NotFoundException("User not found")
                call.respond(updated.toAdminResponse(userRepository))
            }

            get("/users/{id}/detail") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid user id")
                val user = userRepository.findById(id) ?: throw NotFoundException("User not found")

                val quotes = quoteRepository.getUserQuotes(id, includePrivate = true, limit = 20, offset = 0)
                val comments = commentRepository.getForUser(id, limit = 20, offset = 0)

                call.respond(
                    AdminUserDetailResponse(
                        user = user.toAdminResponse(userRepository),
                        recentQuotes = quotes.map { it.toAdminResponse(user.username) },
                        recentComments = comments.map {
                            AdminCommentResponse(
                                id = it.id,
                                quoteId = it.quoteId,
                                text = it.text,
                                createdAt = DateTimeFormatter.ISO_INSTANT.format(it.createdAt)
                            )
                        }
                    )
                )
            }

            get("/analytics") {
                call.requireAdmin(userRepository)
                val days = call.parameters["days"]?.toIntOrNull()?.coerceIn(1, 90) ?: 30
                val counts = analyticsRepository.getDailyCounts(days)
                call.respond(
                    AnalyticsResponse(
                        points = counts.map {
                            AnalyticsPoint(
                                date = it.date.toString(),
                                newUsers = it.newUsers,
                                newQuotes = it.newQuotes,
                                newLikes = it.newLikes,
                                newComments = it.newComments
                            )
                        }
                    )
                )
            }

            get("/quotes/pending") {
                call.requireAdmin(userRepository)
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L
                val quotes = quoteRepository.getByStatus("PENDING", limit, offset)
                call.respond(quotes.map { quote ->
                    val owner = userRepository.findById(quote.ownerId)
                    quote.toAdminResponse(owner?.username ?: "unknown")
                })
            }

            post("/quotes/{id}/approve") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val quote = quoteRepository.setStatus(id, "APPROVED") ?: throw NotFoundException("Quote not found")
                val owner = userRepository.findById(quote.ownerId)
                call.respond(quote.toAdminResponse(owner?.username ?: "unknown"))
            }

            post("/quotes/{id}/reject") {
                val admin = call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val quote = quoteRepository.setStatus(id, "REJECTED") ?: throw NotFoundException("Quote not found")
                notificationRepository.create(
                    recipientUserId = quote.ownerId,
                    actorUserId = admin.id,
                    type = "REJECTED",
                    quoteId = id
                )
                val owner = userRepository.findById(quote.ownerId)
                call.respond(quote.toAdminResponse(owner?.username ?: "unknown"))
            }

            delete("/quotes/{id}") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val deleted = quoteRepository.adminDelete(id)
                if (!deleted) throw NotFoundException("Quote not found")
                call.respond(HttpStatusCode.NoContent)
            }

            get("/settings") {
                call.requireAdmin(userRepository)
                val settings = appSettingsRepository.get()
                call.respond(AppSettingsResponse(settings.defaultAutoApprove))
            }

            put("/settings") {
                call.requireAdmin(userRepository)
                val request = call.receive<UpdateAppSettingsRequest>()
                val settings = appSettingsRepository.setDefaultAutoApprove(request.defaultAutoApprove)
                call.respond(AppSettingsResponse(settings.defaultAutoApprove))
            }

            get("/reports") {
                call.requireAdmin(userRepository)
                val status = call.parameters["status"]?.uppercase() ?: "PENDING"
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L

                val reports = reportRepository.getByStatus(status, limit, offset)
                call.respond(reports.map { report ->
                    val reporter = userRepository.findById(report.reporterId)
                    val preview = when (report.targetType) {
                        "QUOTE" -> quoteRepository.findById(report.targetId)?.text?.take(140)
                        "COMMENT" -> commentRepository.findById(report.targetId)?.text?.take(140)
                        "USER" -> userRepository.findById(report.targetId)?.let { "@${it.username}" }
                        else -> null
                    }
                    ReportResponse(
                        id = report.id,
                        targetType = report.targetType,
                        targetId = report.targetId,
                        reason = report.reason,
                        details = report.details,
                        status = report.status,
                        reporterUsername = reporter?.username ?: "unknown",
                        targetPreview = preview,
                        createdAt = DateTimeFormatter.ISO_INSTANT.format(report.createdAt)
                    )
                })
            }

            post("/reports/{id}/resolve") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid report id")
                reportRepository.updateStatus(id, "RESOLVED")
                call.respond(HttpStatusCode.NoContent)
            }

            post("/reports/{id}/dismiss") {
                call.requireAdmin(userRepository)
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid report id")
                reportRepository.updateStatus(id, "DISMISSED")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
