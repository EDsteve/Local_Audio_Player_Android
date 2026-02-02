package com.localplayer.model

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val folderPath: String,
    val genre: String
)

data class Folder(
    val path: String,
    val name: String,
    val tracks: List<Track>,
    val subfolders: List<Folder> = emptyList()
) {
    /** Returns all tracks in this folder and all subfolders recursively */
    fun allTracksRecursive(): List<Track> {
        val result = mutableListOf<Track>()
        result.addAll(tracks)
        subfolders.forEach { subfolder ->
            result.addAll(subfolder.allTracksRecursive())
        }
        return result
    }

    /** Returns total track count including subfolders */
    fun totalTrackCount(): Int {
        return tracks.size + subfolders.sumOf { it.totalTrackCount() }
    }
}

data class GenreBucket(
    val name: String,
    val tracks: List<Track>
)
