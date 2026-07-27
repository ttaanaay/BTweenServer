package com.btween.app.data.remote.api

import com.btween.app.data.remote.dto.AuthResponseDto
import com.btween.app.data.remote.dto.LoginRequestDto
import com.btween.app.data.remote.dto.RefreshRequestDto
import com.btween.app.data.remote.dto.RegisterRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): AuthResponseDto
}
