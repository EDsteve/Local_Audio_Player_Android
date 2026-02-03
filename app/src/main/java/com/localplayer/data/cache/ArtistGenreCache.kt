package com.localplayer.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching artist-to-genre mappings from Last.fm API.
 * This allows offline-first genre detection after initial lookup.
 */
@Entity(tableName = "artist_genre_cache")
data class ArtistGenreCache(
    /** Normalized artist name (lowercase, trimmed) as primary key */
    @PrimaryKey
    val artistName: String,
    
    /** Genre mapped to our 20-genre taxonomy */
    val genre: String,
    
    /** Raw tags from Last.fm (JSON array string for debugging/future use) */
    val rawTags: String? = null,
    
    /** Timestamp when this entry was created/updated */
    val lastUpdated: Long = System.currentTimeMillis()
)
