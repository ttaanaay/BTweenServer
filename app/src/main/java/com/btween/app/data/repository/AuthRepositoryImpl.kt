package com.btween.app.data.repository

import com.btween.app.data.remote.TokenManager
import com.btween.app.data.remote.api.AuthApi
import com.btween.app.data.remote.dto.LoginRequestDto
import com.btween.app.data.remote.dto.RegisterRequestDto
import com.btween.app.data.remote.dto.toDomain
import com.btween.app.data.remote.safeApiCall
import com.btween.app.domain.model.User
import com.btween.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedIn

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String
    ): Result<User> = safeApiCall {
        val response = authApi.register(RegisterRequestDto(username, email, password, displayName))
        tokenManager.saveSession(response.accessToken, response.refreshToken, response.user.id)
        response.user.toDomain()
    }

    override suspend fun login(email: String, password: String): Result<User> = safeApiCall {
        val response = authApi.login(LoginRequestDto(email, password))
        tokenManager.saveSession(response.accessToken, response.refreshToken, response.user.id)
        response.user.toDomain()
    }

    override fun logout() {
        tokenManager.clearSession()
    }

    override fun getCurrentUserId(): Long? = tokenManager.getUserId()
}
