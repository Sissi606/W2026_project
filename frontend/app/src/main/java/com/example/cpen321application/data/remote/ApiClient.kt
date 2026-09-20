package com.example.cpen321application.data.remote

import com.example.cpen321application.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private val json = Json {
        // The server may add fields later; an unknown one should not crash the
        // app mid-parse.
        ignoreUnknownKeys = true
    }

    fun create(tokenProvider: () -> String?): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            // Headers carry the bearer token, so full logging is debug-only.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
            .client(client)
            .addConverterFactory(json.asConverterFactory(CONTENT_TYPE.toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Retrofit rejects a base URL that does not end in '/', and it is easy to
     * paste one without the slash into local.properties. Normalising here turns
     * a crash at startup into a non-issue.
     */
    internal fun normalizeBaseUrl(raw: String): String =
        if (raw.endsWith("/")) raw else "$raw/"

    private const val TIMEOUT_SECONDS = 15L
    private const val CONTENT_TYPE = "application/json"
}
