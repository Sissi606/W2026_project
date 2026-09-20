package com.example.cpen321application.data.remote

import kotlinx.serialization.Serializable

/**
 * Wire types for the backend API. Field names match the JSON exactly, so no
 * @SerialName annotations are needed.
 */

@Serializable
data class GoogleLoginRequest(val idToken: String)

@Serializable
data class GoogleLoginResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(
    val email: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class ServerIpResponse(val serverIp: String, val clientIp: String)

@Serializable
data class ServerTimeResponse(val serverTime: String)

@Serializable
data class DeveloperResponse(val firstName: String, val lastName: String)

/** Shape of the backend's error responses: `{ "error": "..." }`. */
@Serializable
data class ApiErrorResponse(val error: String)
