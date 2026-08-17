package com.btweeu.server.routes

import com.btweeu.server.data.repository.CategoryRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(val id: Long, val name: String, val icon: String)

fun Route.categoryRoutes(categoryRepository: CategoryRepository) {
    route("/categories") {
        get {
            val categories = categoryRepository.getAll()
            call.respond(categories.map { CategoryResponse(it.id, it.name, it.icon) })
        }
    }
}
