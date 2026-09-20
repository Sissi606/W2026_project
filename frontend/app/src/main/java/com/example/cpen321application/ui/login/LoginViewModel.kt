package com.example.cpen321application.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.auth.GoogleSignInHelper
import com.example.cpen321application.auth.SignInCancelledException
import com.example.cpen321application.data.remote.UserDto
import com.example.cpen321application.data.repository.AppRepository
import com.example.cpen321application.data.repository.BackendInfo
import com.example.cpen321application.util.ClientTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * What the login screen renders. A single immutable object rather than several
 * independent flags, so impossible combinations (loading *and* showing an
 * error) cannot be represented.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val signedInUser: UserDto? = null,
    val info: BackendInfo? = null,
    val clientTime: String? = null,
    val errorMessage: String? = null
) {
    val isSignedIn: Boolean get() = signedInUser != null && info != null
}

class LoginViewModel(
    private val repository: AppRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * @param activityContext the Activity hosting the credential bottom sheet.
     */
    fun signIn(activityContext: Context) {
        _uiState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val idToken = googleSignInHelper.requestIdToken(activityContext)
                val user = repository.signIn(idToken)
                val info = repository.loadInfo()

                // Sampled once the data is in hand, so the two times on screen
                // describe the same moment rather than drifting apart.
                _uiState.value = LoginUiState(
                    signedInUser = user,
                    info = info,
                    clientTime = ClientTime.nowFormatted()
                )
            } catch (e: SignInCancelledException) {
                // Dismissing the picker is a deliberate act, not a failure.
                _uiState.value = LoginUiState()
            } catch (e: Exception) {
                _uiState.value = LoginUiState(errorMessage = describe(e))
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = LoginUiState()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Turns an exception into something a person can act on. A raw
     * `HttpException: HTTP 401` on screen tells the user nothing.
     */
    private fun describe(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "The server rejected the sign-in. Check that GOOGLE_CLIENT_ID matches on both the app and the backend."
            in 500..599 -> "The server had a problem (HTTP ${e.code()}). Try again shortly."
            else -> "Request failed with HTTP ${e.code()}."
        }
        is IOException -> "Cannot reach the server. Check your connection and that the backend is running."
        else -> e.message ?: "Something went wrong."
    }

    companion object {
        fun factory(
            repository: AppRepository,
            googleSignInHelper: GoogleSignInHelper
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LoginViewModel(repository, googleSignInHelper) as T
        }
    }
}
