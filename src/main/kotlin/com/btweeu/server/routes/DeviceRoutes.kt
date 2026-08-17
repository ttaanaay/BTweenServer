package com.btweeu.server.routes

import com.btweeu.server.data.repository.DeviceTokenRepository
import com.btweeu.server.exception.ValidationException
import com.btweeu.server.plugins.AUTH_JWT
import com.btweeu.server.plugins.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(val fcmToken: String)

fun Route.deviceRoutes(deviceTokenRepository: DeviceTokenRepository) {
    route("/devices") {
        authenticate(AUTH_JWT) {
            post("/register") {
                val userId = call.requireUserId()
                val request = call.receive<RegisterDeviceRequest>()
                if (request.fcmToken.isBlank()) throw ValidationException("Missing fcmToken")
                deviceTokenRepository.register(userId, request.fcmToken)
                call.respond(HttpStatusCode.NoContent)
            }

            post("/unregister") {
                call.requireUserId()
                val request = call.receive<RegisterDeviceRequest>()
                deviceTokenRepository.unregister(request.fcmToken)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
