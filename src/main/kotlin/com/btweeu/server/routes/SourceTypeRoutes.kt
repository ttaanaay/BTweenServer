package com.btweeu.server.routes

import com.btweeu.server.data.repository.SourceTypeRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class SourceTypeResponse(val id: Long, val name: String)

fun Route.sourceTypeRoutes(sourceTypeRepository: SourceTypeRepository) {
    route("/source-types") {
        get {
            val sourceTypes = sourceTypeRepository.getAll()
            call.respond(sourceTypes.map { SourceTypeResponse(it.id, it.name) })
        }
    }
}
