package com.finecomputer.grokterm.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.finecomputer.grokterm.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OnboardingStore(private val context: Context) {

    private val KEY_DONE = booleanPreferencesKey("onboarding_complete")

    val isCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DONE] ?: false
    }

    suspend fun isComplete(): Boolean {
        return context.dataStore.data.first()[KEY_DONE] ?: false
    }

    suspend fun setComplete(complete: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DONE] = complete
        }
    }
}
