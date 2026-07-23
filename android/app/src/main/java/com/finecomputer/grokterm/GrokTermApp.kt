package com.finecomputer.grokterm

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "grokterm_prefs")

class GrokTermApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Future: initialize binary manager, logging, etc.
    }
}
