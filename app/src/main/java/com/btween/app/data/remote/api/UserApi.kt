package com.btween.app.data.remote.api

import com.btween.app.data.remote.dto.QuoteResponseDto
import com.btween.app.data.remote.dto.UpdateProfileRequestDto
import com.btween.app.data.remote.dto.UserResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): UserResponseDto

    @PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): UserResponseDto

    @GET("users/{id}/quotes")
    suspend fun getUserQuotes(
        @Path("id") id: Long,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Long = 0
    ): List<QuoteResponseDto>

    @POST("users/{id}/follow")
    suspend fun follow(@Path("id") id: Long)

    @DELETE("users/{id}/follow")
    suspend fun unfollow(@Path("id") id: Long)
}
