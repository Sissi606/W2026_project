package com.example.cpen321application

import android.app.Application
import com.example.cpen321application.auth.GoogleSignInHelper
import com.example.cpen321application.data.local.TokenStore
import com.example.cpen321application.data.remote.ApiClient
import com.example.cpen321application.data.repository.AppRepository

/**
 * Hand-rolled dependency container.
 *
 * The project has no DI framework, and for a handful of singletons one is hard
 * to justify. Everything is built once here and read from the Application
 * instance; swap this for Hilt if the graph grows.
 */
class CPEN321Application : Application() {

    lateinit var repository: AppRepository
        private set

    lateinit var googleSignInHelper: GoogleSignInHelper
        private set

    override fun onCreate() {
        super.onCreate()

        val tokenStore = TokenStore(this)
        // The interceptor reads the token lazily on every request, so a token
        // stored later in the session is picked up without rebuilding OkHttp.
        val api = ApiClient.create(tokenProvider = tokenStore::read)

        repository = AppRepository(api, tokenStore)
        googleSignInHelper = GoogleSignInHelper(BuildConfig.GOOGLE_CLIENT_ID)
    }
}
