package com.localplayer.data

import android.content.Context
import android.net.Uri

/**
 * Manages persistent storage for app preferences including selected music folders.
 */
class PreferencesManager(context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "local_player_prefs"
        private const val KEY_SELECTED_FOLDERS = "selected_folders"
    }
    
    /**
     * Save selected folder URIs to persistent storage.
     */
    fun saveSelectedFolders(folders: List<Uri>) {
        val uriStrings = folders.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_SELECTED_FOLDERS, uriStrings).apply()
    }
    
    /**
     * Load saved folder URIs from persistent storage.
     */
    fun loadSelectedFolders(): List<Uri> {
        val uriStrings = prefs.getStringSet(KEY_SELECTED_FOLDERS, emptySet()) ?: emptySet()
        return uriStrings.mapNotNull { uriString ->
            try {
                Uri.parse(uriString)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Remove a specific folder from saved folders.
     */
    fun removeFolder(folder: Uri) {
        val current = loadSelectedFolders().toMutableList()
        current.remove(folder)
        saveSelectedFolders(current)
    }
    
    /**
     * Clear all saved folders.
     */
    fun clearAllFolders() {
        prefs.edit().remove(KEY_SELECTED_FOLDERS).apply()
    }
}
