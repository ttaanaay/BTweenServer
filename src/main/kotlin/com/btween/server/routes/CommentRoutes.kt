package com.btween.server.routes

import com.btween.server.data.repository.CommentRepository
import com.btween.server.data.repository.NotificationRepository
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.dto.CommentRequest
import com.btween.server.dto.toResponse
import com.btween.server.exception.NotFoundException
import com.btween.server.exception.UnauthorizedException
import com.btween.server.exception.ValidationException
import com.btween.server.plugins.AUTH_JWT
import com.btween.server.plugins.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.commentRoutes(
    commentRepository: CommentRepository,
    quoteRepository: QuoteRepository,
    userRepository: UserRepository,
    notificationRepository: NotificationRepository
) {
    route("/quotes/{quoteId}/comments") {
        authenticate(AUTH_JWT, optional = true) {

            get {
                val quoteId = call.parameters["quoteId"]?.toLongOrNull()
                    ?: throw ValidationException("Invalid quote id")
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L

                val comments = commentRepository.getForQuote(quoteId, limit, offset)
                call.respond(comments.map { comment ->
                    val author = userRepository.findById(comment.userId)
                    comment.toResponse(author?.toResponse(userRepository, null) ?: throw NotFoundException("Author not found"))
                })
            }

            post {
                val userId = call.requireUserId()
                val quoteId = call.parameters["quoteId"]?.toLongOrNull()
                    ?: throw ValidationException("Invalid quote id")
                val request = call.receive<CommentRequest>()
                val text = request.text.trim()
                if (text.isEmpty()) throw ValidationException("Comment can't be empty")
                if (text.length > 1000) throw ValidationException("Comment is too long")

                val quote = quoteRepository.findById(quoteId) ?: throw NotFoundException("Quote not found")
                val comment = commentRepository.create(quoteId, userId, text)

                notificationRepository.create(
                    recipientUserId = quote.ownerId,
                    actorUserId = userId,
                    type = "COMMENT",
                    quoteId = quoteId
                )

                val author = userRepository.findById(userId)!!.toResponse(userRepository, userId)
                call.respond(HttpStatusCode.Created, comment.toResponse(author))
            }
        }
    }

    route("/comments/{id}") {
        authenticate(AUTH_JWT) {
            put {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid comment id")
                val request = call.receive<CommentRequest>()
                val text = request.text.trim()
                if (text.isEmpty()) throw ValidationException("Comment can't be empty")
                if (text.length > 1000) throw ValidationException("Comment is too long")

                val updated = commentRepository.update(id, userId, text)
                    ?: throw UnauthorizedException("Comment not found, or you don't own it")
                val author = userRepository.findById(userId)!!.toResponse(userRepository, userId)
                call.respond(updated.toResponse(author))
            }

            delete {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid comment id")
                val deleted = commentRepository.delete(id, userId)
                if (!deleted) throw UnauthorizedException("Comment not found, or you don't own it")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
