package com.finecomputer.grokterm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.finecomputer.grokterm.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * Secure storage for XAI_API_KEY.
 * Uses EncryptedSharedPreferences for the key itself.
 */
class ApiKeyStore(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "grokterm_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val KEY_API = "xai_api_key"

    fun saveApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_API, key.trim()).apply()
    }

    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API).apply()
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()
}
