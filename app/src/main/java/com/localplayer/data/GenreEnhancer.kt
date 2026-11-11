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
        
        // Rate limiting: Last.fm allows ~5 requests/second; stay well under it
        private const val API_CALL_DELAY_MS = 300L
        
        // Cache "unknown" results to avoid repeated API calls
        private const val UNKNOWN_MARKER = "_UNKNOWN_"

        // Last.fm error code for "artist not found"
        private const val ERROR_ARTIST_NOT_FOUND = 6

        // A failed call is retried after these waits before the artist is given up on
        private val RETRY_WAITS_MS = longArrayOf(3_000L, 10_000L)

        // After this many artists failed in a row even with retries, the network or
        // Last.fm is down: stop calling for a while instead of waiting on every artist
        private const val MAX_FAILURES_IN_A_ROW = 3
        private const val PAUSE_AFTER_FAILURES_MS = 60_000L
    }

    /** Outcome of one Last.fm lookup. */
    private sealed interface Lookup {
        /** Last.fm returned tags and a genre was picked. */
        data class Found(val genre: String) : Lookup
        /** Last.fm answered, but does not know the artist or has no tags for it. */
        object NoGenre : Lookup
        /** The call itself failed (network, rate limit, server error). Worth retrying later. */
        object Failed : Lookup
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

    // Artists that failed in a row, and until when calls are paused because of them
    private var failuresInARow = 0
    private var pausedUntil = 0L
    
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
            return@withContext GenreMapper.specificGenre(metadataGenre)
        }
        
        val normalizedArtist = ArtistParser.normalizeArtistName(artist)
        
        // Check cache first (instant)
        val cached = cacheDao.getCacheEntry(normalizedArtist)
        if (cached != null) {
            // If we cached an "unknown" result, fall back to metadata genre
            if (cached.genre == UNKNOWN_MARKER) {
                return@withContext GenreMapper.specificGenre(metadataGenre)
            }
            hasLoggedOffline = false // Reset offline flag when using cache
            // Pick again from the saved Last.fm tags, so a rescan applies the
            // current genre tree without another API call
            val savedTags = cached.rawTags?.split(", ")?.filter { it.isNotBlank() }
            return@withContext if (savedTags.isNullOrEmpty()) {
                GenreMapper.displayName(cached.genre)
            } else {
                pickGenre(savedTags)
            }
        }
        
        // Check network before attempting API call
        if (!isNetworkAvailable()) {
            if (!hasLoggedOffline) {
                Log.d(TAG, "Network unavailable (offline/doze mode), using fallback genre")
                hasLoggedOffline = true
            }
            // Don't cache - will retry when online
            return@withContext GenreMapper.specificGenre(metadataGenre)
        }
        
        hasLoggedOffline = false // Reset offline flag when online
        
        // Not in cache and online - try API
        when (val lookup = fetchGenreFromApi(normalizedArtist, artist)) {
            is Lookup.Found -> return@withContext lookup.genre
            // Last.fm has nothing for this artist: remember that, so we don't ask again
            Lookup.NoGenre -> cacheUnknownArtist(normalizedArtist)
            // The call failed: don't save anything, so the next scan tries again
            Lookup.Failed -> Unit
        }

        return@withContext GenreMapper.specificGenre(metadataGenre)
    }
    
    /**
     * Look up an artist on Last.fm, retrying when the call itself fails.
     * A found genre is saved in the cache.
     */
    private suspend fun fetchGenreFromApi(normalizedArtist: String, originalArtist: String): Lookup {
        return apiMutex.withLock {
            if (System.currentTimeMillis() < pausedUntil) {
                return@withLock Lookup.Failed
            }

            var lookup = callLastFm(normalizedArtist, originalArtist)
            // A failed call is usually a rate limit or a short network drop: wait and try again
            for (waitMs in RETRY_WAITS_MS) {
                if (lookup != Lookup.Failed) break
                Log.w(TAG, "Lookup failed for '$originalArtist', trying again in ${waitMs / 1000}s")
                delay(waitMs)
                lookup = callLastFm(normalizedArtist, originalArtist)
            }

            if (lookup == Lookup.Failed) {
                failuresInARow++
                if (failuresInARow >= MAX_FAILURES_IN_A_ROW) {
                    Log.w(TAG, "$failuresInARow artists failed in a row, pausing lookups for ${PAUSE_AFTER_FAILURES_MS / 1000}s")
                    pausedUntil = System.currentTimeMillis() + PAUSE_AFTER_FAILURES_MS
                    failuresInARow = 0
                }
            } else {
                failuresInARow = 0
            }

            lookup
        }
    }

    /**
     * One call to Last.fm for an artist.
     */
    private suspend fun callLastFm(normalizedArtist: String, originalArtist: String): Lookup {
        return try {
            // Rate limiting
            val timeSinceLastCall = System.currentTimeMillis() - lastApiCallTime
            if (timeSinceLastCall < API_CALL_DELAY_MS) {
                delay(API_CALL_DELAY_MS - timeSinceLastCall)
            }

            Log.d(TAG, "Fetching genre from Last.fm for: $originalArtist")

            val response = lastFmApi.getArtistInfo(artist = originalArtist)
            lastApiCallTime = System.currentTimeMillis()

            val body = response.body()
            if (!response.isSuccessful || body == null) {
                Log.w(TAG, "Last.fm API error: ${response.code()} - ${response.message()}")
                return Lookup.Failed
            }
            if (body.error != null) {
                // Last.fm reports errors (unknown artist, rate limit, ...) in an HTTP 200 body
                Log.w(TAG, "Last.fm error ${body.error} for '$originalArtist': ${body.message}")
                return if (body.error == ERROR_ARTIST_NOT_FOUND) Lookup.NoGenre else Lookup.Failed
            }

            // Last.fm corrects spellings ("Seals and Crofts" -> "Seals & Crofts"), which is
            // what we want, but it can also jump to a different artist ("Tony Ann" -> "Tony An")
            val returnedName = body.artist?.name
            if (returnedName != null && !ArtistParser.isSameArtist(originalArtist, returnedName)) {
                Log.d(TAG, "Ignoring Last.fm match '$returnedName' for '$originalArtist': different artist")
                return Lookup.NoGenre
            }

            val tags = body.artist?.tags?.tag?.mapNotNull { it.name } ?: emptyList()
            if (tags.isEmpty()) {
                return Lookup.NoGenre
            }

            // Pick the genre from the Last.fm tags
            val genre = pickGenre(tags)

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
            Lookup.Found(genre)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching genre from Last.fm: ${e.message}")
            Lookup.Failed
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
     * Pick a genre from an artist's Last.fm tags (most used first).
     * Prefers the first tag that belongs to the genre tree, so "indie rock"
     * wins over an earlier "seen live" or "icelandic". If none does, falls back
     * to the first tag that is not a nationality, decade or similar.
     */
    private fun pickGenre(tags: List<String>): String {
        tags.firstOrNull { GenreMapper.isKnown(it) }?.let {
            return GenreMapper.displayName(it)
        }
        
        val fallback = tags.firstOrNull { tag ->
            val words = " ${tag.lowercase().trim()} "
            // Whole words only: "uk" must not rule out "punk"
            tag.trim().length >= 3 && ignoredTags.none { words.contains(" $it ") }
        }
        
        return fallback?.let { GenreMapper.displayName(it) } ?: GenreMapper.OTHER
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
     * Forget the artists saved as "no genre found", so the next scan looks them up again.
     * Artists that already have a genre are kept.
     */
    suspend fun forgetUnknownArtists() = withContext(Dispatchers.IO) {
        cacheDao.deleteByGenre(UNKNOWN_MARKER)
        pausedUntil = 0L
        failuresInARow = 0
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
