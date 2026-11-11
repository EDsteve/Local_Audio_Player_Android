package com.localplayer.data

import android.content.Context

/**
 * Genres the user set by hand, for one track or for every track of an artist.
 * They win over the genre from the file or from Last.fm and survive rescans.
 */
class GenreOverrides(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "genre_overrides"

        fun trackKey(trackId: String): String = "track:$trackId"

        fun artistKey(artist: String): String =
            "artist:${ArtistParser.normalizeArtistName(artist)}"

        /**
         * The genre set by hand for a track: its own if it has one, otherwise
         * its artist's. Null if neither was set.
         */
        fun manualGenre(overrides: Map<String, String>, trackId: String, artist: String): String? =
            overrides[trackKey(trackId)] ?: overrides[artistKey(artist)]
    }

    /** Everything set by hand, keyed by [trackKey] or [artistKey]. */
    fun all(): Map<String, String> =
        prefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()

    fun setForTrack(trackId: String, genre: String) {
        prefs.edit().putString(trackKey(trackId), genre).apply()
    }

    /**
     * Set the genre for an artist. Genres set earlier on single tracks of that
     * artist ([trackIds]) are dropped, so all of them follow the artist.
     */
    fun setForArtist(artist: String, genre: String, trackIds: List<String>) {
        val editor = prefs.edit()
        trackIds.forEach { editor.remove(trackKey(it)) }
        editor.putString(artistKey(artist), genre).apply()
    }

    /** Back to the automatic genre: forget what was set for the track and its artist. */
    fun clear(trackId: String, artist: String) {
        prefs.edit().remove(trackKey(trackId)).remove(artistKey(artist)).apply()
    }
}
