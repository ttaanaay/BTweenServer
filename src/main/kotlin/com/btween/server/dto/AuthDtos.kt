package com.btween.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class OAuthLoginRequest(
    val token: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String
)

@Serializable
<<<<<<< HEAD
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class DeleteAccountRequest(
    val password: String
)

@Serializable
=======
>>>>>>> fe4f7e9d2c2a154c775d63dc7c950d4ea9f1a006
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)
