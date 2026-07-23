package com.finecomputer.grokterm.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finecomputer.grokterm.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Remembers the user-selected project / Production Bible directory via SAF.
 * Stores the tree URI and display name; takes persistable read/write permission.
 */
class ProjectStore(private val context: Context) {

    private val KEY_URI = stringPreferencesKey("project_tree_uri")
    private val KEY_NAME = stringPreferencesKey("project_display_name")

    val projectUriFlow: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[KEY_URI]?.let { Uri.parse(it) }
    }

    val projectNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_NAME]
    }

    suspend fun getProjectUri(): Uri? {
        return context.dataStore.data.first()[KEY_URI]?.let { Uri.parse(it) }
    }

    suspend fun getProjectName(): String? {
        return context.dataStore.data.first()[KEY_NAME]
    }

    suspend fun setProject(uri: Uri, displayName: String?) {
        // Take persistable permission so it survives restarts
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            // Some providers only grant read; still useful
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }

        context.dataStore.edit { prefs ->
            prefs[KEY_URI] = uri.toString()
            prefs[KEY_NAME] = displayName ?: uri.lastPathSegment ?: "Selected folder"
        }
    }

    suspend fun clearProject() {
        val existing = getProjectUri()
        if (existing != null) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    existing,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_URI)
            prefs.remove(KEY_NAME)
        }
    }

    /**
     * Best-effort real filesystem path for ProcessBuilder.directory().
     * Many devices still expose /storage/emulated/0/... via the last path segment.
     * Returns null if we cannot derive a usable path (pure SAF content URI).
     */
    fun tryResolveFilesystemPath(uri: Uri): String? {
        // Common pattern: content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FMyBible
        val docId = uri.lastPathSegment ?: return null
        if (docId.startsWith("primary:")) {
            val relative = docId.removePrefix("primary:").replace("%2F", "/").replace("%3A", ":")
            return "/storage/emulated/0/$relative"
        }
        // Fallback: raw path if it looks like one
        if (docId.startsWith("/")) return docId
        return null
    }
}
