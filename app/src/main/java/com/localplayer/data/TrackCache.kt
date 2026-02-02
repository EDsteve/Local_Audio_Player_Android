package com.localplayer.data

import android.content.Context
import android.net.Uri
import com.localplayer.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Caches the track library to avoid rescanning on every app restart.
 */
class TrackCache(private val context: Context) {
    
    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)
    
    companion object {
        private const val CACHE_FILE_NAME = "track_cache.json"
    }
    
    /**
     * Save tracks to cache file.
     */
    fun saveTracks(tracks: List<Track>) {
        try {
            val jsonArray = JSONArray()
            tracks.forEach { track ->
                val jsonObject = JSONObject().apply {
                    put("id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("album", track.album)
                    put("durationMs", track.durationMs)
                    put("contentUri", track.contentUri.toString())
                    put("folderPath", track.folderPath)
                    put("genre", track.genre)
                }
                jsonArray.put(jsonObject)
            }
            cacheFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load tracks from cache file.
     * Returns null if cache doesn't exist or is invalid.
     */
    fun loadTracks(): List<Track>? {
        return try {
            if (!cacheFile.exists()) return null
            
            val jsonString = cacheFile.readText()
            if (jsonString.isBlank()) return null
            
            val jsonArray = JSONArray(jsonString)
            val tracks = mutableListOf<Track>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                tracks.add(
                    Track(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.getString("album"),
                        durationMs = obj.getLong("durationMs"),
                        contentUri = Uri.parse(obj.getString("contentUri")),
                        folderPath = obj.getString("folderPath"),
                        genre = obj.getString("genre")
                    )
                )
            }
            
            if (tracks.isEmpty()) null else tracks
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if cache exists and is valid.
     */
    fun hasCache(): Boolean {
        return cacheFile.exists() && cacheFile.length() > 0
    }
    
    /**
     * Clear the cache (forces rescan on next load).
     */
    fun clearCache() {
        try {
            cacheFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
