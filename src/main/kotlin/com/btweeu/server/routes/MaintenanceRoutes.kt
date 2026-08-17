package com.btweeu.server.routes

import com.btweeu.server.data.repository.AppSettingsRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class MaintenanceStatusResponse(val enabled: Boolean, val message: String?)

fun Route.maintenanceRoutes(appSettingsRepository: AppSettingsRepository) {
    route("/maintenance-status") {
        get {
            val settings = appSettingsRepository.get()
            call.respond(MaintenanceStatusResponse(settings.maintenanceMode, settings.maintenanceMessage))
        }
    }
}
