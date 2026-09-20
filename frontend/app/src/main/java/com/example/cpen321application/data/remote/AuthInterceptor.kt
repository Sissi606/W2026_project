package com.example.cpen321application.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the session token to outgoing requests.
 *
 * The token is read through a lambda rather than captured by value because it
 * changes at sign-in, and the OkHttp client is built once for the process.
 * Requests made before sign-in simply go out without the header, and the
 * backend answers 401.
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
