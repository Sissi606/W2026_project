package com.example.cpen321application.data.repository

import com.example.cpen321application.data.local.TokenStore
import com.example.cpen321application.data.remote.ApiService
import com.example.cpen321application.data.remote.GoogleLoginRequest
import com.example.cpen321application.data.remote.RandomFactResponse
import com.example.cpen321application.data.remote.UserDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Everything the info screen displays, gathered in one place. */
data class BackendInfo(
    val serverIp: String,
    val clientIp: String,
    val serverTime: String,
    val developerFirstName: String,
    val developerLastName: String
)

class AppRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {

    /**
     * Exchanges the Google ID token for a session token and stores it, so the
     * interceptor can attach it to the calls that follow.
     */
    suspend fun signIn(googleIdToken: String): UserDto {
        val response = api.googleLogin(GoogleLoginRequest(googleIdToken))
        tokenStore.write(response.token)
        return response.user
    }

    /**
     * The three authenticated calls run concurrently rather than in sequence:
     * they are independent, so waiting for each in turn would triple the time
     * the user spends looking at a spinner.
     */
    suspend fun loadInfo(): BackendInfo = coroutineScope {
        val ipDeferred = async { api.getServerIp() }
        val timeDeferred = async { api.getServerTime() }
        val developerDeferred = async { api.getDeveloper() }

        val ip = ipDeferred.await()
        val time = timeDeferred.await()
        val developer = developerDeferred.await()

        BackendInfo(
            serverIp = ip.serverIp,
            clientIp = ip.clientIp,
            serverTime = time.serverTime,
            developerFirstName = developer.firstName,
            developerLastName = developer.lastName
        )
    }

    /** Button 3's surprise. Needs no session, by design. */
    suspend fun loadRandomFact(): RandomFactResponse = api.getRandomFact()

    fun signOut() = tokenStore.clear()

    fun hasSession(): Boolean = !tokenStore.read().isNullOrBlank()
}
