package com.btweeu.server.routes

import com.btweeu.server.data.repository.CollectionRepository
import com.btweeu.server.data.repository.QuoteRepository
import com.btweeu.server.data.repository.UserRepository
import com.btweeu.server.dto.AddItemRequest
import com.btweeu.server.dto.CollectionDetailResponse
import com.btweeu.server.dto.CollectionRequest
import com.btweeu.server.dto.toResponse
import com.btweeu.server.exception.NotFoundException
import com.btweeu.server.exception.UnauthorizedException
import com.btweeu.server.exception.ValidationException
import com.btweeu.server.plugins.AUTH_JWT
import com.btweeu.server.plugins.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.format.DateTimeFormatter

fun Route.collectionRoutes(
    collectionRepository: CollectionRepository,
    quoteRepository: QuoteRepository,
    userRepository: UserRepository
) {
    route("/collections") {
        authenticate(AUTH_JWT) {

            get {
                val userId = call.requireUserId()
                val collections = collectionRepository.getForUser(userId)
                call.respond(collections.map { collection ->
                    val quoteIds = collectionRepository.getQuoteIds(collection.id)
                    val coverImageUrl = quoteIds.firstNotNullOfOrNull { quoteRepository.findById(it)?.imageUrl }
                    collection.toResponse(quoteIds.size, coverImageUrl)
                })
            }

            post {
                val userId = call.requireUserId()
                val request = call.receive<CollectionRequest>()
                val name = request.name.trim()
                if (name.isEmpty()) throw ValidationException("Collection name can't be empty")
                if (name.length > 100) throw ValidationException("Collection name is too long")

                val collection = collectionRepository.create(userId, name)
                call.respond(HttpStatusCode.Created, collection.toResponse(0))
            }

            get("/{id}") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid collection id")
                val collection = collectionRepository.findById(id) ?: throw NotFoundException("Collection not found")
                if (collection.ownerId != userId) throw UnauthorizedException("This isn't your collection")

                val quoteIds = collectionRepository.getQuoteIds(id)
                val quotes = quoteIds.mapNotNull { quoteRepository.findById(it) }
                val likedIds = quoteRepository.likedQuoteIds(userId, quotes.map { it.id })

                call.respond(
                    CollectionDetailResponse(
                        id = collection.id,
                        name = collection.name,
                        quotes = quotes.map { quote ->
                            val owner = userRepository.findById(quote.ownerId)!!.toResponse(userRepository, userId)
                            quote.toResponse(owner, quote.id in likedIds)
                        },
                        createdAt = DateTimeFormatter.ISO_INSTANT.format(collection.createdAt)
                    )
                )
            }

            delete("/{id}") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid collection id")
                val deleted = collectionRepository.delete(id, userId)
                if (!deleted) throw UnauthorizedException("Collection not found, or you don't own it")
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/items") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid collection id")
                val collection = collectionRepository.findById(id) ?: throw NotFoundException("Collection not found")
                if (collection.ownerId != userId) throw UnauthorizedException("This isn't your collection")

                val request = call.receive<AddItemRequest>()
                quoteRepository.findById(request.quoteId) ?: throw NotFoundException("Quote not found")
                collectionRepository.addItem(id, request.quoteId)
                call.respond(HttpStatusCode.NoContent)
            }

            delete("/{id}/items/{quoteId}") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException("Invalid collection id")
                val quoteId = call.parameters["quoteId"]?.toLongOrNull() ?: throw ValidationException("Invalid quote id")
                val collection = collectionRepository.findById(id) ?: throw NotFoundException("Collection not found")
                if (collection.ownerId != userId) throw UnauthorizedException("This isn't your collection")

                collectionRepository.removeItem(id, quoteId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
