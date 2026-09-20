package com.example.cpen321application.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds the session token between app launches.
 *
 * The token is a bearer credential: anyone holding it can call the API as this
 * user until it expires, so it is kept in EncryptedSharedPreferences (backed by
 * a key in the Android Keystore) rather than plain SharedPreferences.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun read(): String? = prefs.getString(KEY_TOKEN, null)

    fun write(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val FILE_NAME = "session_prefs"
        const val KEY_TOKEN = "session_token"
    }
}
