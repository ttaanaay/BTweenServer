package com.btween.server.routes

import com.btween.server.config.AppConfig
import com.btween.server.data.repository.EmailVerificationRepository
import com.btween.server.data.repository.PasswordResetRepository
import com.btween.server.data.repository.RefreshTokenRepository
import com.btween.server.data.repository.RefreshTokenStatus
import com.btween.server.data.repository.UserRepository
import com.btween.server.domain.User
import com.btween.server.dto.AuthResponse
import com.btween.server.dto.ChangePasswordRequest
import com.btween.server.dto.ForgotPasswordRequest
import com.btween.server.dto.LoginRequest
import com.btween.server.dto.MessageResponse
import com.btween.server.dto.OAuthLoginRequest
import com.btween.server.dto.RefreshRequest
import com.btween.server.dto.RegisterRequest
import com.btween.server.dto.ResetPasswordRequest
import com.btween.server.dto.ResendVerificationRequest
import com.btween.server.dto.VerifyCodeRequest
import com.btween.server.dto.VerifyEmailRequest
import com.btween.server.dto.toResponse
import com.btween.server.email.EmailSender
import com.btween.server.exception.ConflictException
import com.btween.server.exception.NotFoundException
import com.btween.server.exception.UnauthorizedException
import com.btween.server.exception.ValidationException
import com.btween.server.plugins.AUTH_JWT
import com.btween.server.plugins.AUTH_RATE_LIMIT
import com.btween.server.plugins.requireUserId
import com.btween.server.security.FacebookTokenVerifier
import com.btween.server.security.GoogleTokenVerifier
import com.btween.server.security.JwtService
import com.btween.server.security.MicrosoftTokenVerifier
import com.btween.server.security.OAuthProfile
import com.btween.server.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

// Usernames that could make someone look like BTween staff, an official account, or a
// moderator - reserved so a regular signup can't impersonate one. Checked case-insensitively
// and after stripping underscores/digits, so "Admin", "admin_", and "admin123" are all
// caught, not just an exact "admin" match.
private val RESERVED_USERNAMES = setOf(
    "admin", "administrator", "root", "superuser", "sysadmin",
    "moderator", "mod", "staff", "support", "helpdesk", "help",
    "official", "btween", "btweenofficial", "btweenteam", "btweensupport", "btweenapp",
    "security", "system", "webmaster", "owner", "founder", "ceo",
    "team", "verified", "bot", "service", "noreply", "notification", "notifications"
)

private fun isReservedUsername(username: String): Boolean {
    val normalized = username.lowercase().filter { it.isLetter() }
    return RESERVED_USERNAMES.any { reserved -> normalized == reserved || normalized.startsWith(reserved) }
}

fun Route.authRoutes(
    userRepository: UserRepository,
    jwtService: JwtService,
    googleVerifier: GoogleTokenVerifier?,
    facebookVerifier: FacebookTokenVerifier,
    microsoftVerifier: MicrosoftTokenVerifier,
    passwordResetRepository: PasswordResetRepository,
    emailVerificationRepository: EmailVerificationRepository,
    refreshTokenRepository: RefreshTokenRepository,
    emailSender: EmailSender,
    config: AppConfig
) {
    route("/auth") {
        rateLimit(AUTH_RATE_LIMIT) {

            post("/register") {
                val request = call.receive<RegisterRequest>()

                if (!USERNAME_REGEX.matches(request.username)) {
                    throw ValidationException("Username must be 3-20 characters: letters, numbers, underscore only")
                }
                if (isReservedUsername(request.username)) {
                    throw ValidationException("That username isn't available")
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

                val code = emailVerificationRepository.createCode(user.id)
                emailSender.send(
                    to = user.email,
                    subject = "Verify your BTween email",
                    body = "Your verification code is: $code\nIt expires in 15 minutes."
                )

                // Deliberately no tokens here - the account exists (so the username/email
                // are reserved and can't be grabbed by someone else mid-verification) but
                // isn't usable until /auth/verify-registration succeeds below. That's what
                // actually logs the person in for the first time.
                call.respond(
                    HttpStatusCode.Created,
                    RegistrationPendingResponse(
                        email = user.email,
                        message = "We've sent a verification code to ${user.email}. Enter it to finish creating your account."
                    )
                )
            }

            post("/verify-registration") {
                val request = call.receive<VerifyEmailRequest>()
                val user = userRepository.findByEmail(request.email)
                    ?: throw ValidationException("Invalid or expired code")

                if (user.emailVerified) {
                    // Already verified (e.g. a retried/duplicate request) - just log them
                    // in rather than erroring, since the outcome the person wants either
                    // way is "get me into my account".
                    call.respond(buildAuthResponse(user, jwtService, userRepository, refreshTokenRepository, config))
                    return@post
                }

                val valid = emailVerificationRepository.verifyAndConsumeCode(user.id, request.code)
                if (!valid) throw ValidationException("Invalid or expired code")

                userRepository.markEmailVerified(user.id)
                val verifiedUser = userRepository.findById(user.id) ?: user
                call.respond(buildAuthResponse(verifiedUser, jwtService, userRepository, refreshTokenRepository, config))
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                val user = userRepository.findByEmail(request.email)
                    ?: throw UnauthorizedException("Incorrect email or password")

                val lockedUntil = user.lockedUntil
                if (lockedUntil != null && lockedUntil.isAfter(java.time.Instant.now())) {
                    throw UnauthorizedException("Too many failed attempts - try again later")
                }

                if (user.passwordHash == null) {
                    throw UnauthorizedException(
                        "This account signs in with ${user.authProvider?.lowercase() ?: "a social account"} - use that instead"
                    )
                }
                if (!PasswordHasher.verify(request.password, user.passwordHash)) {
                    userRepository.recordFailedLogin(user.id)
                    throw UnauthorizedException("Incorrect email or password")
                }
                if (user.isBanned) {
                    throw UnauthorizedException("This account has been suspended")
                }

                userRepository.resetFailedLogins(user.id)
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository, refreshTokenRepository, config))
            }

            post("/refresh") {
                val request = call.receive<RefreshRequest>()
                val userId = jwtService.verifyRefreshToken(request.refreshToken)
                    ?: throw UnauthorizedException("Refresh token is invalid or expired")

                when (refreshTokenRepository.checkStatus(request.refreshToken)) {
                    RefreshTokenStatus.REVOKED -> {
                        // This exact token was already rotated away or logged out - being
                        // presented again is a strong signal it was copied/stolen, so kill
                        // every active session for this account as a precaution.
                        refreshTokenRepository.revokeAllForUser(userId)
                        throw UnauthorizedException("This session has been revoked - please log in again")
                    }
                    RefreshTokenStatus.EXPIRED -> throw UnauthorizedException("Refresh token is invalid or expired")
                    RefreshTokenStatus.UNKNOWN -> throw UnauthorizedException("Refresh token is invalid or expired")
                    RefreshTokenStatus.VALID -> { /* proceed */ }
                }

                val user = userRepository.findById(userId)
                    ?: throw UnauthorizedException("Account no longer exists")
                if (user.isBanned) {
                    throw UnauthorizedException("This account has been suspended")
                }

                // Rotation: the old refresh token is single-use - revoke it now that a new
                // one is about to be issued in its place.
                refreshTokenRepository.revoke(request.refreshToken)
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository, refreshTokenRepository, config))
            }

            post("/logout") {
                val request = call.receive<RefreshRequest>()
                // No need to verify the token is still valid/unexpired first - revoking an
                // already-expired or already-revoked token is a harmless no-op either way,
                // and this endpoint only accepts a token the caller already possesses.
                refreshTokenRepository.revoke(request.refreshToken)
                call.respond(HttpStatusCode.OK, MessageResponse("Logged out"))
            }

            post("/oauth/google") {
                val verifier = googleVerifier
                    ?: throw ValidationException("Google sign-in isn't configured on this server yet")
                val request = call.receive<OAuthLoginRequest>()
                val profile = verifier.verify(request.token)
                    ?: throw UnauthorizedException("Invalid Google token")
                val user = findOrCreateOAuthUser(userRepository, "GOOGLE", profile)
                if (user.isBanned) throw UnauthorizedException("This account has been suspended")
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository, refreshTokenRepository, config))
            }

            post("/oauth/facebook") {
                val request = call.receive<OAuthLoginRequest>()
                val profile = facebookVerifier.verify(request.token)
                    ?: throw UnauthorizedException("Invalid Facebook token")
                val user = findOrCreateOAuthUser(userRepository, "FACEBOOK", profile)
                if (user.isBanned) throw UnauthorizedException("This account has been suspended")
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository, refreshTokenRepository, config))
            }

            post("/oauth/microsoft") {
                val request = call.receive<OAuthLoginRequest>()
                val profile = microsoftVerifier.verify(request.token)
                    ?: throw UnauthorizedException("Invalid Microsoft token")
                val user = findOrCreateOAuthUser(userRepository, "MICROSOFT", profile)
                if (user.isBanned) throw UnauthorizedException("This account has been suspended")
                call.respond(HttpStatusCode.OK, buildAuthResponse(user, jwtService, userRepository, refreshTokenRepository, config))
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

            post("/verify-email") {
                val request = call.receive<VerifyEmailRequest>()
                val user = userRepository.findByEmail(request.email)
                    ?: throw ValidationException("Invalid or expired code")

                val valid = emailVerificationRepository.verifyAndConsumeCode(user.id, request.code)
                if (!valid) throw ValidationException("Invalid or expired code")

                userRepository.markEmailVerified(user.id)
                call.respond(HttpStatusCode.OK, MessageResponse("Email verified"))
            }

            post("/resend-verification") {
                val request = call.receive<ResendVerificationRequest>()
                val user = userRepository.findByEmail(request.email)

                // Same "always respond the same way" pattern as forgot-password, so this
                // can't be used to probe which emails have accounts.
                if (user != null && !user.emailVerified) {
                    val code = emailVerificationRepository.createCode(user.id)
                    emailSender.send(
                        to = user.email,
                        subject = "Verify your BTween email",
                        body = "Your verification code is: $code\nIt expires in 15 minutes."
                    )
                }
                call.respond(HttpStatusCode.OK, MessageResponse("If that email needs verifying, a new code has been sent."))
            }

            authenticate(AUTH_JWT) {
                // Session-based counterparts of the two endpoints above - for a logged-in
                // person, the server already knows which account it's verifying from the
                // token, so there's no reason to ask them to type their own email (and no
                // way for them to accidentally target a different account by mistyping it).
                post("/verify-email-me") {
                    val userId = call.requireUserId()
                    val request = call.receive<VerifyCodeRequest>()
                    val valid = emailVerificationRepository.verifyAndConsumeCode(userId, request.code)
                    if (!valid) throw ValidationException("Invalid or expired code")
                    userRepository.markEmailVerified(userId)
                    call.respond(HttpStatusCode.OK, MessageResponse("Email verified"))
                }

                post("/resend-verification-me") {
                    val userId = call.requireUserId()
                    val user = userRepository.findById(userId) ?: throw NotFoundException("User not found")
                    if (!user.emailVerified) {
                        val code = emailVerificationRepository.createCode(user.id)
                        emailSender.send(
                            to = user.email,
                            subject = "Verify your BTween email",
                            body = "Your verification code is: $code\nIt expires in 15 minutes."
                        )
                    }
                    call.respond(HttpStatusCode.OK, MessageResponse("A new code has been sent."))
                }
            }

            authenticate(AUTH_JWT) {
                post("/change-password") {
                    val userId = call.requireUserId()
                    val request = call.receive<ChangePasswordRequest>()
                    val user = userRepository.findById(userId) ?: throw NotFoundException("User not found")

                    if (user.passwordHash == null) {
                        throw ValidationException("This account signed in with Google/Facebook/Microsoft and has no password to change")
                    }
                    if (!PasswordHasher.verify(request.currentPassword, user.passwordHash)) {
                        throw UnauthorizedException("Current password is incorrect")
                    }
                    if (request.newPassword.length < 8) {
                        throw ValidationException("New password must be at least 8 characters")
                    }

                    userRepository.updatePassword(userId, PasswordHasher.hash(request.newPassword))
                    call.respond(HttpStatusCode.OK, MessageResponse("Password changed"))
                }
            }
        }
    }
}

private fun buildAuthResponse(
    user: User,
    jwtService: JwtService,
    userRepository: UserRepository,
    refreshTokenRepository: RefreshTokenRepository,
    config: AppConfig
): AuthResponse {
    val accessToken = jwtService.generateAccessToken(user.id)
    val refreshToken = jwtService.generateRefreshToken(user.id)
    refreshTokenRepository.store(
        userId = user.id,
        rawToken = refreshToken,
        expiresAt = java.time.Instant.now().plusSeconds(config.jwtRefreshTokenExpiryDays * 86_400L)
    )
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
        .let { if (isReservedUsername(it)) "user" else it }
    var username = baseUsername
    var suffix = 0
    while (userRepository.usernameTaken(username) || isReservedUsername(username)) {
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
