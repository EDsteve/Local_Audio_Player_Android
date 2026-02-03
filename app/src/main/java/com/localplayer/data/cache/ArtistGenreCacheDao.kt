package com.localplayer.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for artist-genre cache operations.
 * Provides methods to query and store artist genre mappings.
 */
@Dao
interface ArtistGenreCacheDao {
    
    /**
     * Get the cached genre for an artist.
     * Returns null if artist is not in cache.
     */
    @Query("SELECT genre FROM artist_genre_cache WHERE artistName = :artistName LIMIT 1")
    suspend fun getGenreForArtist(artistName: String): String?
    
    /**
     * Get the full cache entry for an artist.
     * Useful for debugging or checking raw tags.
     */
    @Query("SELECT * FROM artist_genre_cache WHERE artistName = :artistName LIMIT 1")
    suspend fun getCacheEntry(artistName: String): ArtistGenreCache?
    
    /**
     * Insert or update an artist-genre mapping.
     * Uses REPLACE strategy to update existing entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtistGenre(cache: ArtistGenreCache)
    
    /**
     * Insert multiple artist-genre mappings at once.
     * Useful for batch operations.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<ArtistGenreCache>)
    
    /**
     * Check if an artist exists in the cache.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM artist_genre_cache WHERE artistName = :artistName LIMIT 1)")
    suspend fun hasArtist(artistName: String): Boolean
    
    /**
     * Get the number of cached artists.
     * Useful for statistics/debugging.
     */
    @Query("SELECT COUNT(*) FROM artist_genre_cache")
    suspend fun getCacheCount(): Int
    
    /**
     * Clear all cached entries.
     * Use with caution - forces re-fetch from API.
     */
    @Query("DELETE FROM artist_genre_cache")
    suspend fun clearAll()
    
    /**
     * Clear entries older than a specified timestamp.
     * Useful for cache maintenance.
     */
    @Query("DELETE FROM artist_genre_cache WHERE lastUpdated < :olderThan")
    suspend fun clearOlderThan(olderThan: Long)
    
    /**
     * Get all cached artist names.
     * Useful for debugging.
     */
    @Query("SELECT artistName FROM artist_genre_cache")
    suspend fun getAllCachedArtists(): List<String>
}
