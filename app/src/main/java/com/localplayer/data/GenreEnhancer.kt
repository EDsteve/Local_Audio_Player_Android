package com.localplayer.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.localplayer.data.cache.ArtistGenreCache
import com.localplayer.data.cache.ArtistGenreCacheDao
import com.localplayer.data.cache.GenreDatabase
import com.localplayer.data.lastfm.LastFmApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Service for enhanced genre detection using Last.fm API with local caching.
 * Implements offline-first strategy: checks cache before API calls.
 * 
 * Usage:
 * 1. Check cache for known artist
 * 2. If not cached and online, query Last.fm API
 * 3. Map Last.fm tags to our 20-genre taxonomy
 * 4. Cache result for future lookups
 */
class GenreEnhancer(private val context: Context) {
    
    companion object {
        private const val TAG = "GenreEnhancer"
        
        // Rate limiting: Last.fm allows ~5 requests/second
        private const val API_CALL_DELAY_MS = 200L
        
        // Cache "unknown" results to avoid repeated API calls
        private const val UNKNOWN_MARKER = "_UNKNOWN_"
    }
    
    private val cacheDao: ArtistGenreCacheDao = GenreDatabase.getInstance(context).artistGenreCacheDao()
    private val lastFmApi: LastFmApi = LastFmApi.create()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Mutex to prevent concurrent API calls for the same artist
    private val apiMutex = Mutex()
    
    // Track last API call time for rate limiting
    private var lastApiCallTime = 0L
    
    // Track if we've logged the offline state (to avoid spam)
    private var hasLoggedOffline = false
    
    /**
     * Check if network is available.
     * Handles Doze mode and background restrictions.
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network: ${e.message}")
            false
        }
    }
    
    /**
     * Get genre for an artist using cache-first strategy.
     * 
     * @param artist The artist name
     * @param metadataGenre Fallback genre from file metadata
     * @return Genre from our 20-genre taxonomy
     */
    suspend fun getGenreForArtist(artist: String, metadataGenre: String?): String = withContext(Dispatchers.IO) {
        // Skip for unknown artists
        if (artist.isBlank() || artist.lowercase() == "unknown") {
            return@withContext GenreMapper.mapToTopGenre(metadataGenre)
        }
        
        val normalizedArtist = ArtistParser.normalizeArtistName(artist)
        
        // Check cache first (instant)
        val cachedGenre = cacheDao.getGenreForArtist(normalizedArtist)
        if (cachedGenre != null) {
            // If we cached an "unknown" result, fall back to metadata genre
            if (cachedGenre == UNKNOWN_MARKER) {
                return@withContext GenreMapper.mapToTopGenre(metadataGenre)
            }
            hasLoggedOffline = false // Reset offline flag when using cache
            return@withContext cachedGenre
        }
        
        // Check network before attempting API call
        if (!isNetworkAvailable()) {
            if (!hasLoggedOffline) {
                Log.d(TAG, "Network unavailable (offline/doze mode), using fallback genre")
                hasLoggedOffline = true
            }
            // Don't cache - will retry when online
            return@withContext GenreMapper.mapToTopGenre(metadataGenre)
        }
        
        hasLoggedOffline = false // Reset offline flag when online
        
        // Not in cache and online - try API
        val apiGenre = fetchGenreFromApi(normalizedArtist, artist)
        if (apiGenre != null) {
            return@withContext apiGenre
        }
        
        // API failed or artist not found - use metadata genre and cache the failure
        val fallbackGenre = GenreMapper.mapToTopGenre(metadataGenre)
        cacheUnknownArtist(normalizedArtist)
        
        return@withContext fallbackGenre
    }
    
    /**
     * Fetch genre from Last.fm API and cache the result.
     */
    private suspend fun fetchGenreFromApi(normalizedArtist: String, originalArtist: String): String? {
        return apiMutex.withLock {
            try {
                // Rate limiting
                val timeSinceLastCall = System.currentTimeMillis() - lastApiCallTime
                if (timeSinceLastCall < API_CALL_DELAY_MS) {
                    delay(API_CALL_DELAY_MS - timeSinceLastCall)
                }
                
                Log.d(TAG, "Fetching genre from Last.fm for: $originalArtist")
                
                val response = lastFmApi.getArtistInfo(artist = originalArtist)
                lastApiCallTime = System.currentTimeMillis()
                
                if (response.isSuccessful) {
                    val artistInfo = response.body()?.artist
                    val tags = artistInfo?.tags?.tag?.mapNotNull { it.name } ?: emptyList()
                    
                    if (tags.isNotEmpty()) {
                        // Get first valid genre tag from Last.fm
                        val genre = getFirstValidGenre(tags)
                        
                        // Cache the result
                        cacheDao.insertArtistGenre(
                            ArtistGenreCache(
                                artistName = normalizedArtist,
                                genre = genre,
                                rawTags = tags.joinToString(", "),
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                        
                        Log.d(TAG, "Cached genre '$genre' for artist '$originalArtist' (tags: ${tags.take(3).joinToString()})")
                        return@withLock genre
                    }
                } else {
                    Log.w(TAG, "Last.fm API error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching genre from Last.fm: ${e.message}")
            }
            
            null
        }
    }
    
    // Tags to ignore - nationalities, decades, genders, and other non-genre descriptors
    private val ignoredTags = setOf(
        // Nationalities
        "swedish", "british", "american", "german", "french", "japanese", "korean",
        "norwegian", "finnish", "italian", "spanish", "brazilian", "australian",
        "canadian", "irish", "dutch", "belgian", "russian", "polish", "austrian",
        "uk", "usa",
        // Genders/Descriptors
        "female vocalists", "male vocalists", "female vocalist", "male vocalist",
        "female", "male", "vocalist", "vocalists", "singer", "artists",
        // Decades
        "60s", "70s", "80s", "90s", "00s", "2000s", "2010s", "2020s",
        "1960s", "1970s", "1980s", "1990s",
        // Other non-genre descriptors
        "seen live", "favorites", "favourite", "favorites", "love", "loved",
        "my music", "under 2000 listeners", "spotify", "beautiful", "awesome",
        "cool", "good", "great", "best", "albums i own", "top 100"
    )
    
    /**
     * Get the first valid genre tag from Last.fm.
     * Returns the tag directly (capitalized) without mapping to predefined genres.
     */
    private fun getFirstValidGenre(tags: List<String>): String {
        // Filter out non-genre tags first
        val genreTags = tags.filter { tag ->
            val lowerTag = tag.lowercase()
            // Skip ignored tags
            if (ignoredTags.any { ignored -> lowerTag == ignored || lowerTag.contains(ignored) }) {
                return@filter false
            }
            // Skip very short tags (likely not useful)
            if (lowerTag.length < 3) {
                return@filter false
            }
            true
        }
        
        Log.d(TAG, "Filtered tags: ${genreTags.take(5).joinToString()}")
        
        // Return the first valid tag, properly capitalized
        if (genreTags.isNotEmpty()) {
            return capitalizeGenre(genreTags.first())
        }
        
        return "Other"
    }
    
    /**
     * Capitalize genre name properly.
     * Examples: "hip-hop" -> "Hip-Hop", "r&b" -> "R&B", "rock" -> "Rock"
     */
    private fun capitalizeGenre(genre: String): String {
        val lower = genre.lowercase()
        
        // Special cases
        return when {
            lower == "r&b" || lower == "rnb" -> "R&B"
            lower == "hip-hop" || lower == "hip hop" -> "Hip-Hop"
            lower == "edm" -> "EDM"
            lower == "j-pop" || lower == "jpop" -> "J-Pop"
            lower == "k-pop" || lower == "kpop" -> "K-Pop"
            lower == "lo-fi" || lower == "lofi" -> "Lo-Fi"
            lower.contains("-") -> {
                // Capitalize each part: "singer-songwriter" -> "Singer-Songwriter"
                genre.split("-").joinToString("-") { part ->
                    part.replaceFirstChar { it.uppercase() }
                }
            }
            else -> genre.replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Cache an "unknown" result to avoid repeated API calls.
     */
    private suspend fun cacheUnknownArtist(normalizedArtist: String) {
        try {
            cacheDao.insertArtistGenre(
                ArtistGenreCache(
                    artistName = normalizedArtist,
                    genre = UNKNOWN_MARKER,
                    rawTags = null,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error caching unknown artist: ${e.message}")
        }
    }
    
    /**
     * Pre-cache genres for a batch of artists.
     * Useful for initial library scan optimization.
     */
    suspend fun prefetchGenresForArtists(artists: Set<String>) = withContext(Dispatchers.IO) {
        val unknownArtists = artists.filter { artist ->
            val normalized = ArtistParser.normalizeArtistName(artist)
            !cacheDao.hasArtist(normalized)
        }
        
        Log.d(TAG, "Prefetching genres for ${unknownArtists.size} uncached artists")
        
        for (artist in unknownArtists) {
            if (artist.isNotBlank() && artist.lowercase() != "unknown") {
                getGenreForArtist(artist, null)
            }
        }
    }
    
    /**
     * Get cache statistics.
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        CacheStats(
            totalCached = cacheDao.getCacheCount(),
            cachedArtists = cacheDao.getAllCachedArtists()
        )
    }
    
    /**
     * Clear the entire cache.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDao.clearAll()
    }
    
    data class CacheStats(
        val totalCached: Int,
        val cachedArtists: List<String>
    )
}
