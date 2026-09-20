package com.example.cpen321application.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    /** Exchanges a Google ID token for one of our own. Needs no auth header. */
    @POST("api/auth/google")
    suspend fun googleLogin(@Body body: GoogleLoginRequest): GoogleLoginResponse

    // The three endpoints below require the session token. AuthInterceptor
    // attaches it, so callers do not pass it explicitly.

    @GET("api/info/server-ip")
    suspend fun getServerIp(): ServerIpResponse

    @GET("api/info/server-time")
    suspend fun getServerTime(): ServerTimeResponse

    @GET("api/info/developer")
    suspend fun getDeveloper(): DeveloperResponse
}
