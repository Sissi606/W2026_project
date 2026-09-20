package com.example.cpen321application.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Raised when the user dismisses the account picker; not an error to report. */
class SignInCancelledException : Exception("Sign-in cancelled")

/**
 * Obtains a Google ID token through Credential Manager, the current
 * (non-deprecated) replacement for GoogleSignInClient.
 *
 * The token is only useful to the backend, which verifies it against Google's
 * public keys. Nothing here decides whether the user is authenticated -- that
 * is entirely the server's call.
 */
class GoogleSignInHelper(private val serverClientId: String) {

    /**
     * @param activityContext must be an Activity: Credential Manager renders a
     *   system bottom sheet and cannot do so from an application context.
     */
    suspend fun requestIdToken(activityContext: Context): String {
        if (serverClientId.isBlank()) {
            throw IllegalStateException(
                "GOOGLE_CLIENT_ID is empty. Set it in local.properties and rebuild."
            )
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            // false shows every Google account on the device. Filtering to
            // previously authorized accounts shows nothing on a first run,
            // which reads as a broken button.
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            // The user picks an account explicitly rather than being signed in
            // silently, so the demo always shows the picker.
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = try {
            CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelledException()
        } catch (e: NoCredentialException) {
            throw IllegalStateException(
                "No Google account on this device. Add one in Settings, then try again.",
                e
            )
        } catch (e: GetCredentialException) {
            // Most often a misconfigured OAuth client: wrong SHA-1, wrong
            // package name, or an Android client that was never created.
            throw IllegalStateException(
                "Google sign-in failed (${e.type}). Check the OAuth client setup.",
                e
            )
        }

        val credential = response.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Unexpected credential type: ${credential.type}")
        }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
