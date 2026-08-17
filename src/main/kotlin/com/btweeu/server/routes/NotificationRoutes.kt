package com.btweeu.server.routes

import com.btweeu.server.data.repository.NotificationRepository
import com.btweeu.server.data.repository.QuoteRepository
import com.btweeu.server.data.repository.UserRepository
import com.btweeu.server.dto.NotificationResponse
import com.btweeu.server.dto.UnreadCountResponse
import com.btweeu.server.exception.NotFoundException
import com.btweeu.server.exception.ValidationException
import com.btweeu.server.plugins.AUTH_JWT
import com.btweeu.server.plugins.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.format.DateTimeFormatter

fun Route.notificationRoutes(
    notificationRepository: NotificationRepository,
    userRepository: UserRepository,
    quoteRepository: QuoteRepository
) {
    route("/notifications") {
        authenticate(AUTH_JWT) {

            get {
                val userId = call.requireUserId()
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L

                val notifications = notificationRepository.getForUser(userId, limit, offset)
                call.respond(notifications.map { notification ->
                    val actor = userRepository.findById(notification.actorUserId)
                    val quotePreview = notification.quoteId?.let { quoteRepository.findById(it)?.text?.take(60) }
                    NotificationResponse(
                        id = notification.id,
                        type = notification.type,
                        actorId = notification.actorUserId,
                        actorUsername = actor?.username ?: "unknown",
                        actorDisplayName = actor?.displayName ?: "Unknown user",
                        quoteId = notification.quoteId,
                        quoteTextPreview = quotePreview,
                        isRead = notification.isRead,
                        createdAt = DateTimeFormatter.ISO_INSTANT.format(notification.createdAt)
                    )
                })
            }

            get("/unread-count") {
                val userId = call.requireUserId()
                call.respond(UnreadCountResponse(notificationRepository.countUnread(userId)))
            }

            post("/{id}/read") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid notification id")
                val updated = notificationRepository.markRead(id, userId)
                if (!updated) throw NotFoundException("Notification not found")
                call.respond(HttpStatusCode.NoContent)
            }

            post("/read-all") {
                val userId = call.requireUserId()
                notificationRepository.markAllRead(userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
