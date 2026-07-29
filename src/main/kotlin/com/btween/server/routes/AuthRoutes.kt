package com.btween.server.routes

import com.btween.server.data.repository.PasswordResetRepository
import com.btween.server.data.repository.UserRepository
import com.btween.server.domain.User
import com.btween.server.dto.AuthResponse
import com.btween.server.dto.ForgotPasswordRequest
import com.btween.server.dto.LoginRequest
import com.btween.server.dto.MessageResponse
import com.btween.server.dto.OAuthLoginRequest
import com.btween.server.dto.RefreshRequest
import com.btween.server.dto.RegisterRequest
import com.btween.server.dto.ResetPasswordRequest
import com.btween.server.dto.toResponse
import com.btween.server.email.EmailSender
import com.btween.server.exception.ConflictException
import com.btween.server.exception.UnauthorizedException
import com.btween.server.exception.ValidationException
import com.btween.server.plugins.AUTH_RATE_LIMIT
import com.btween.server.security.FacebookTokenVerifier
import com.btween.server.security.GoogleTokenVerifier
import com.btween.server.security.JwtService
import com.btween.server.security.MicrosoftTokenVerifier
import com.btween.server.security.OAuthProfile
import com.btween.server.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun Route.authRoutes(
    userRepository: UserRepository,
    jwtService: JwtService,
    googleVerifier: GoogleTokenVerifier?,
    facebookVerifier: FacebookTokenVerifier,
    microsoftVerifier: MicrosoftTokenVerifier,
    passwordResetRepository: PasswordResetRepository,
    emailSender: EmailSender
) {
    route("/auth") {
        rateLimit(AUTH_RATE_LIMIT) {

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
                if (request.displayName.trim().length == 0) {
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
                call.respond(HttpStatusCode.Created, buildAuthResponse(user, jwtService, userRepository))
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                val user = userRepository.findByEmail(request.email)
                    ?: throw UnauthorizedException("Incorrect email or password")

                if (user.passwordHash == null) {
                    throw UnauthorizedException(
                        "This account signs in with ${user.authProvider?.lowercase() ?: "a social account"} - use that instead"
                    )
                }
                if (!PasswordHasher.verify(request.password, user.passwordHash)) {
                    throw UnauthorizedException("Incorrect email or password")
                }
                if (user.isBanned) {
                    throw UnauthorizedException("This account has been suspended")
                }

                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository))
            }

            post("/refresh") {
                val request = call.receive<RefreshRequest>()
                val userId = jwtService.verifyRefreshToken(request.refreshToken)
                    ?: throw UnauthorizedException("Refresh token is invalid or expired")
                val user = userRepository.findById(userId)
                    ?: throw UnauthorizedException("Account no longer exists")
                if (user.isBanned) {
                    throw UnauthorizedException("This account has been suspended")
                }

                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository))
            }

            post("/oauth/google") {
                val verifier = googleVerifier
                    ?: throw ValidationException("Google sign-in isn't configured on this server yet")
                val request = call.receive<OAuthLoginRequest>()
                val profile = verifier.verify(request.token)
                    ?: throw UnauthorizedException("Invalid Google token")
                val user = findOrCreateOAuthUser(userRepository, "GOOGLE", profile)
                if (user.isBanned) throw UnauthorizedException("This account has been suspended")
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository))
            }

            post("/oauth/facebook") {
                val request = call.receive<OAuthLoginRequest>()
                val profile = facebookVerifier.verify(request.token)
                    ?: throw UnauthorizedException("Invalid Facebook token")
                val user = findOrCreateOAuthUser(userRepository, "FACEBOOK", profile)
                if (user.isBanned) throw UnauthorizedException("This account has been suspended")
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository))
            }

            post("/oauth/microsoft") {
                val request = call.receive<OAuthLoginRequest>()
                val profile = microsoftVerifier.verify(request.token)
                    ?: throw UnauthorizedException("Invalid Microsoft token")
                val user = findOrCreateOAuthUser(userRepository, "MICROSOFT", profile)
                if (user.isBanned) throw UnauthorizedException("This account has been suspended")
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository))
            }

            post("/forgot-password") {
                val request = call.receive<ForgotPasswordRequest>()
                val user = userRepository.findByEmail(request.email)

                // Deliberately always respond the same way whether or not the email exists -
                // otherwise this endpoint could be used to check which emails have accounts.
                if (user != null && user.passwordHash != null) {
                    val code = passwordResetRepository.createCode(user.id)
                    emailSender.send(
                        to = user.email,
                        subject = "Your BTween password reset code",
                        body = "Your password reset code is: $code\nIt expires in 15 minutes."
                    )
                }
                call.respond(
                    HttpStatusCode.OK,
                    MessageResponse("If that email has an account, a reset code has been sent.")
                )
            }

            post("/reset-password") {
                val request = call.receive<ResetPasswordRequest>()
                if (request.newPassword.length < 8) {
                    throw ValidationException("Password must be at least 8 characters")
                }
                val user = userRepository.findByEmail(request.email)
                    ?: throw ValidationException("Invalid or expired code")

                val valid = passwordResetRepository.verifyAndConsumeCode(user.id, request.code)
                if (!valid) throw ValidationException("Invalid or expired code")

                userRepository.updatePassword(user.id, PasswordHasher.hash(request.newPassword))
                call.respond(HttpStatusCode.OK, MessageResponse("Password updated"))
            }
        }
    }
}

private fun buildAuthResponse(user: User, jwtService: JwtService, userRepository: UserRepository): AuthResponse {
    val accessToken = jwtService.generateAccessToken(user.id)
    val refreshToken = jwtService.generateRefreshToken(user.id)
    return AuthResponse(accessToken, refreshToken, user.toResponse(userRepository, user.id))
}

/**
 * Finds the account this OAuth sign-in belongs to, creating a brand-new one if this is the
 * person's first time signing in this way. If an existing *local* (email/password) account
 * already uses this exact email, that account is returned as-is rather than creating a
 * duplicate - the person effectively gains a second way to sign into the same account.
 */
private fun findOrCreateOAuthUser(userRepository: UserRepository, provider: String, profile: OAuthProfile): User {
    userRepository.findByProvider(provider, profile.providerUserId)?.let { return it }

    val email = profile.email
        ?: throw ValidationException("Your $provider account has no email on file, so we can't create an account")

    userRepository.findByEmail(email)?.let { return it }

    val baseUsername = email.substringBefore("@")
        .filter { it.isLetterOrDigit() || it == '_' }
        .take(15)
        .ifBlank { "user" }
    var username = baseUsername
    var suffix = 0
    while (userRepository.usernameTaken(username)) {
        suffix++
        username = "$baseUsername$suffix"
    }

    return userRepository.createOAuthUser(
        provider = provider,
        providerUserId = profile.providerUserId,
        username = username,
        email = email,
        displayName = profile.name?.takeIf { it.isNotBlank() } ?: baseUsername
    )
}
