package com.btween.app.domain.repository

import com.btween.app.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    val isLoggedIn: StateFlow<Boolean>

    suspend fun register(username: String, email: String, password: String, displayName: String): Result<User>

    suspend fun login(email: String, password: String): Result<User>

    fun logout()

    fun getCurrentUserId(): Long?
}
