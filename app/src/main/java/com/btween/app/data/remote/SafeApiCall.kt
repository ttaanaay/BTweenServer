package com.btween.app.data.remote

import com.btween.app.data.remote.dto.ErrorResponseDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs [block], and if it throws an [HttpException] returned by the BTween API, extracts the
 * server's `{"message": "..."}` body into a friendly [Result.failure] instead of a generic
 * "HTTP 400" message. Any other exception (network failure, timeout, serialization error)
 * is wrapped with a generic, user-safe message.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val message = e.response()?.errorBody()?.string()?.let { body ->
        runCatching { errorJson.decodeFromString(ErrorResponseDto.serializer(), body).message }.getOrNull()
    } ?: "Something went wrong (${e.code()})"
    Result.failure(Exception(message))
} catch (e: Exception) {
    Result.failure(Exception("Couldn't connect. Check your internet connection and try again."))
}
