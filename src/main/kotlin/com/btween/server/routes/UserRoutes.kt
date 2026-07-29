package com.btween.server.routes

import com.btween.server.data.repository.NotificationRepository
import com.btween.server.data.repository.QuoteRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.dto.QuoteResponse
import com.btween.server.dto.TopContributorResponse
import com.btween.server.dto.UpdateProfileRequest
import com.btween.server.dto.UserResponse
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

fun Route.userRoutes(
    userRepository: UserRepository,
    quoteRepository: QuoteRepository,
    notificationRepository: NotificationRepository
) {
    userProfileGetRoutes(userRepository, quoteRepository)
    userProfileMutationRoutes(userRepository, notificationRepository)
}

private fun Route.userProfileGetRoutes(userRepository: UserRepository, quoteRepository: QuoteRepository) {
    route("/users") {
        authenticate(AUTH_JWT, optional = true) {
            get("/top-contributors") {
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10
                val contributors = userRepository.getTopContributors(limit)
                call.respond(contributors.map { (user, count) ->
                    TopContributorResponse(user = user.toResponse(userRepository, viewerId), quoteCount = count)
                })
            }

            get("/search") {
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                val query = call.parameters["q"]?.trim().orEmpty()
                if (query.length < 2) {
                    call.respond(emptyList<UserResponse>())
                } else {
                    val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
                    val results = userRepository.search(query, limit)
                    call.respond(results.map { it.toResponse(userRepository, viewerId) })
                }
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException("Invalid user id")
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                val user = userRepository.findById(id) ?: throw NotFoundException("User not found")
                call.respond(user.toResponse(userRepository, viewerId))
            }

            get("/{id}/quotes") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException("Invalid user id")
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L

                val owner = userRepository.findById(id) ?: throw NotFoundException("User not found")
                val ownerResponse = owner.toResponse(userRepository, viewerId)
                val quotes = quoteRepository.getUserQuotes(
                    ownerId = id,
                    includePrivate = viewerId == id,
                    limit = limit,
                    offset = offset
                )
                val likedIds = viewerId?.let { quoteRepository.likedQuoteIds(it, quotes.map { q -> q.id }) } ?: emptySet()

                call.respond(quotes.map { quote ->
                    QuoteResponse(
                        id = quote.id, text = quote.text, sourceTitle = quote.sourceTitle,
                        sourceType = quote.sourceType, speaker = quote.speaker, author = quote.author,
                        category = quote.category, tags = quote.tags, visibility = quote.visibility,
                        status = quote.status,
                        likeCount = quote.likeCount, isLikedByMe = quote.id in likedIds,
                        owner = ownerResponse,
                        createdAt = quote.createdAt.toString(), updatedAt = quote.updatedAt.toString()
                    )
                })
            }

            get("/{id}/followers") {
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid user id")
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                userRepository.findById(id) ?: throw NotFoundException("User not found")
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 30
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L
                val followers = userRepository.getFollowers(id, limit, offset)
                call.respond(followers.map { it.toResponse(userRepository, viewerId) })
            }

            get("/{id}/following") {
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid user id")
                val viewerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                userRepository.findById(id) ?: throw NotFoundException("User not found")
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 30
                val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L
                val following = userRepository.getFollowing(id, limit, offset)
                call.respond(following.map { it.toResponse(userRepository, viewerId) })
            }
        }
    }
}

private fun Route.userProfileMutationRoutes(userRepository: UserRepository, notificationRepository: NotificationRepository) {
    route("/users") {
        authenticate(AUTH_JWT, optional = true) {
            put("/me") {
                val userId = call.requireUserId()
                val request = call.receive<UpdateProfileRequest>()
                val newDisplayName = request.displayName?.trim()
                val updated = userRepository.updateProfile(
                    id = userId,
                    displayName = if (newDisplayName != null && newDisplayName.length > 0) newDisplayName else null,
                    avatarUrl = request.avatarUrl,
                    bio = request.bio
                ) ?: throw NotFoundException("User not found")
                call.respond(updated.toResponse(userRepository, userId))
            }

            post("/{id}/follow") {
                val userId = call.requireUserId()
                val targetId = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException("Invalid user id")
                userRepository.findById(targetId) ?: throw NotFoundException("User not found")
                userRepository.follow(userId, targetId)
                notificationRepository.create(
                    recipientUserId = targetId,
                    actorUserId = userId,
                    type = "FOLLOW",
                    quoteId = null
                )
                call.respond(HttpStatusCode.NoContent)
            }

            delete("/{id}/follow") {
                val userId = call.requireUserId()
                val targetId = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException("Invalid user id")
                userRepository.unfollow(userId, targetId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
