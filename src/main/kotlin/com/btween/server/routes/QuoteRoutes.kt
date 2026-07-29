package com.btween.server.routes

import com.btween.server.data.repository.AppSettingsRepository
import com.btween.server.data.repository.NotificationRepository
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.dto.QuoteRequest
import com.btween.server.dto.QuoteResponse
import com.btween.server.dto.toResponse
import com.btween.server.exception.NotFoundException
import com.btween.server.exception.ValidationException
import com.btween.server.plugins.AUTH_JWT
import com.btween.server.plugins.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val ALLOWED_SOURCE_TYPES = setOf(
    "MOVIE", "TV_SERIES", "BOOK", "ANIME", "GAME", "PODCAST", "SPEECH", "OTHER"
)
private val ALLOWED_VISIBILITY = setOf("PUBLIC", "PRIVATE")

private fun validateQuoteRequest(request: QuoteRequest) {
    if (request.text.trim().length == 0) throw ValidationException("Quote text can't be empty")
    if (request.sourceTitle.trim().length == 0) throw ValidationException("Source title can't be empty")
    if (request.speaker.trim().length == 0) throw ValidationException("Speaker / character can't be empty")
    if (request.sourceType.uppercase() !in ALLOWED_SOURCE_TYPES) {
        throw ValidationException("Invalid source type: ${request.sourceType}")
    }
    if (request.visibility.uppercase() !in ALLOWED_VISIBILITY) {
        throw ValidationException("Visibility must be PUBLIC or PRIVATE")
    }
}

fun Route.quoteRoutes(
    quoteRepository: QuoteRepository,
    userRepository: UserRepository,
    appSettingsRepository: AppSettingsRepository,
    notificationRepository: NotificationRepository
) {
    route("/quotes") {

        authenticate(AUTH_JWT, optional = true) {
            get("/feed") {
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L
                val scope = call.parameters["scope"] ?: "recommended"

                val quotes = if (scope == "following") {
                    if (viewerId == null) throw ValidationException("Login required for the following feed")
                    quoteRepository.getFollowingFeed(viewerId, limit, offset)
                } else {
                    quoteRepository.getPublicFeed(limit, offset)
                }
                val likedIds = viewerId?.let { quoteRepository.likedQuoteIds(it, quotes.map { q -> q.id }) } ?: emptySet()
                val ownersById = quotes.map { it.ownerId }.distinct()
                    .associateWith { userRepository.findById(it)!!.toResponse(userRepository, viewerId) }

                call.respond(quotes.map { quote ->
                    quote.toResponse(owner = ownersById.getValue(quote.ownerId), isLikedByMe = quote.id in likedIds)
                })
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                val quote = quoteRepository.findById(id) ?: throw NotFoundException("Quote not found")

                if (quote.visibility == "PRIVATE" && quote.ownerId != viewerId) {
                    throw NotFoundException("Quote not found")
                }

                val owner = userRepository.findById(quote.ownerId)!!.toResponse(userRepository, viewerId)
                val isLiked = viewerId?.let { quoteRepository.likedQuoteIds(it, listOf(quote.id)).size > 0 } ?: false
                call.respond(quote.toResponse(owner, isLiked))
            }
        }

        authenticate(AUTH_JWT, optional = false) {
            post {
                val userId = call.requireUserId()
                val request = call.receive<QuoteRequest>()
                validateQuoteRequest(request)

                val owner = userRepository.findById(userId) ?: throw NotFoundException("Account no longer exists")
                val autoApprove = owner.autoApprove ?: appSettingsRepository.get().defaultAutoApprove
                val initialStatus = if (autoApprove) "APPROVED" else "PENDING"

                val quote = quoteRepository.create(
                    ownerId = userId,
                    text = request.text.trim(),
                    sourceTitle = request.sourceTitle.trim(),
                    sourceType = request.sourceType.uppercase(),
                    speaker = request.speaker.trim(),
                    author = request.author?.trim()?.takeIf { it.length > 0 },
                    category = request.category?.trim()?.takeIf { it.length > 0 },
                    tags = request.tags,
                    imageUrl = request.imageUrl?.trim()?.takeIf { it.length > 0 },
                    visibility = request.visibility.uppercase(),
                    status = initialStatus
                )
                val ownerResponse = owner.toResponse(userRepository, userId)
                call.respond(HttpStatusCode.Created, quote.toResponse(ownerResponse, isLikedByMe = false))
            }

            put("/{id}") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val request = call.receive<QuoteRequest>()
                validateQuoteRequest(request)

                val updated = quoteRepository.update(
                    id = id,
                    ownerId = userId,
                    text = request.text.trim(),
                    sourceTitle = request.sourceTitle.trim(),
                    sourceType = request.sourceType.uppercase(),
                    speaker = request.speaker.trim(),
                    author = request.author?.trim()?.takeIf { it.length > 0 },
                    category = request.category?.trim()?.takeIf { it.length > 0 },
                    tags = request.tags,
                    imageUrl = request.imageUrl?.trim()?.takeIf { it.length > 0 },
                    visibility = request.visibility.uppercase()
                ) ?: throw NotFoundException("Quote not found, or you don't own it")

                val owner = userRepository.findById(userId)!!.toResponse(userRepository, userId)
                val isLiked = quoteRepository.likedQuoteIds(userId, listOf(id)).size > 0
                call.respond(updated.toResponse(owner, isLiked))
            }

            delete("/{id}") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val deleted = quoteRepository.delete(id, userId)
                if (!deleted) throw NotFoundException("Quote not found, or you don't own it")
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/like") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val quote = quoteRepository.findById(id) ?: throw NotFoundException("Quote not found")
                quoteRepository.like(userId, id)
                notificationRepository.create(
                    recipientUserId = quote.ownerId,
                    actorUserId = userId,
                    type = "LIKE",
                    quoteId = id
                )
                call.respond(HttpStatusCode.NoContent)
            }

            delete("/{id}/like") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                quoteRepository.unlike(userId, id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
