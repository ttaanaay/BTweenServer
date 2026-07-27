package com.btween.server.routes

import com.btween.server.data.repository.UserRepository
import com.btween.server.dto.AuthResponse
import com.btween.server.dto.LoginRequest
import com.btween.server.dto.RefreshRequest
import com.btween.server.dto.RegisterRequest
import com.btween.server.dto.toResponse
import com.btween.server.exception.ConflictException
import com.btween.server.exception.UnauthorizedException
import com.btween.server.exception.ValidationException
import com.btween.server.security.JwtService
import com.btween.server.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun Route.authRoutes(userRepository: UserRepository, jwtService: JwtService) {
    route("/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()

            if (!USERNAME_REGEX.matches(request.username)) {
                throw ValidationException("Username must be 3-20 characters: letters, numbers, underscore only")
            }
            if (!EMAIL_REGEX.matches(request.email)) {
                throw ValidationException("Please enter a valid email address")
            }
            if (request.password.length < 8) {
                throw ValidationException("Password must be at least 8 characters")
            }
            if (request.displayName.isBlank()) {
                throw ValidationException("Display name can't be empty")
            }
            if (userRepository.usernameTaken(request.username)) {
                throw ConflictException("Username \"${request.username}\" is already taken")
            }
            if (userRepository.emailTaken(request.email)) {
                throw ConflictException("An account with this email already exists")
            }

            val user = userRepository.create(
                username = request.username,
                email = request.email,
                passwordHash = PasswordHasher.hash(request.password),
                displayName = request.displayName.trim()
            )

            val accessToken = jwtService.generateAccessToken(user.id)
            val refreshToken = jwtService.generateRefreshToken(user.id)
            call.respond(
                HttpStatusCode.Created,
                AuthResponse(accessToken, refreshToken, user.toResponse(userRepository, user.id))
            )
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val user = userRepository.findByEmail(request.email)
                ?: throw UnauthorizedException("Incorrect email or password")

            if (!PasswordHasher.verify(request.password, user.passwordHash)) {
                throw UnauthorizedException("Incorrect email or password")
            }

            val accessToken = jwtService.generateAccessToken(user.id)
            val refreshToken = jwtService.generateRefreshToken(user.id)
            call.respond(
                HttpStatusCode.OK,
                AuthResponse(accessToken, refreshToken, user.toResponse(userRepository, user.id))
            )
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val userId = jwtService.verifyRefreshToken(request.refreshToken)
                ?: throw UnauthorizedException("Refresh token is invalid or expired")
            val user = userRepository.findById(userId)
                ?: throw UnauthorizedException("Account no longer exists")

            val accessToken = jwtService.generateAccessToken(user.id)
            val refreshToken = jwtService.generateRefreshToken(user.id)
            call.respond(
                HttpStatusCode.OK,
                AuthResponse(accessToken, refreshToken, user.toResponse(userRepository, user.id))
            )
        }
    }
}
