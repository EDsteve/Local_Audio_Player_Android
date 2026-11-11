package com.localplayer.ui

import android.app.Application
import android.net.Uri
import androidx.media3.common.Player
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localplayer.data.ArtistParser
import com.localplayer.data.GenreOverrides
import com.localplayer.data.MusicRepository
import com.localplayer.data.PreferencesManager
import com.localplayer.data.TrackCache
import com.localplayer.model.Folder
import com.localplayer.model.GenreBucket
import com.localplayer.model.Track
import com.localplayer.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val rootFolder: Folder? = null, // Single root folder with subfolders + tracks
    val genres: List<GenreBucket> = emptyList(),
    // Genres set by hand, keyed as in GenreOverrides
    val manualGenres: Map<String, String> = emptyMap(),
    val selectedFolders: List<Uri> = emptyList(),
    val nowPlaying: Track? = null,
    val playingQueue: List<Track> = emptyList(),
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val playbackManager = PlaybackManager(application)
    private val preferencesManager = PreferencesManager(application)
    private val trackCache = TrackCache(application)
    private val genreOverrides = GenreOverrides(application)

    // The library with its automatic genres, before the ones set by hand are applied
    private var libraryTracks: List<Track> = emptyList()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Load saved folders and try to load from cache (no scan)
        loadSavedFoldersFromCache()
        
        playbackManager.setListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val directTrack = mediaItem?.localConfiguration?.tag as? Track
                val resolvedTrack = directTrack ?: mediaItem?.mediaId?.let { id ->
                    _uiState.value.playingQueue.firstOrNull { it.id == id }
                }
                if (resolvedTrack != null) {
                    _uiState.value = _uiState.value.copy(nowPlaying = resolvedTrack)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val queue = _uiState.value.playingQueue
                val index = newPosition.mediaItemIndex
                if (index in queue.indices) {
                    _uiState.value = _uiState.value.copy(nowPlaying = queue[index])
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.value = _uiState.value.copy(isShuffleEnabled = shuffleModeEnabled)
            }
        })
        playbackManager.connect()
    }

    /**
     * Load saved folders and try to load tracks from cache.
     * Only scans if cache is empty.
     */
    private fun loadSavedFoldersFromCache() {
        viewModelScope.launch {
            val savedFolders = preferencesManager.loadSelectedFolders()
            if (savedFolders.isEmpty()) return@launch
            
            _uiState.value = _uiState.value.copy(selectedFolders = savedFolders)
            
            // Try to load from cache first (instant)
            val cachedTracks = trackCache.loadTracks()
            if (cachedTracks != null) {
                // Use cached tracks - no scanning needed
                showLibrary(cachedTracks)
            } else {
                // No cache - need to scan
                scanAndCacheLibrary()
            }
        }
    }

    fun setSelectedFolders(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(selectedFolders = uris)
        // Save folders to persistent storage
        preferencesManager.saveSelectedFolders(uris)
        // Clear cache and rescan since folders changed
        trackCache.clearCache()
        scanAndCacheLibrary()
    }
    
    /**
     * Remove a folder from the library and persistent storage.
     */
    fun removeFolder(uri: Uri) {
        val updated = _uiState.value.selectedFolders.filter { it != uri }
        _uiState.value = _uiState.value.copy(selectedFolders = updated)
        preferencesManager.saveSelectedFolders(updated)
        // Clear cache and rescan since folders changed
        trackCache.clearCache()
        scanAndCacheLibrary()
    }

    /**
     * Force a full rescan of the library (manual refresh).
     * Use this when user wants to refresh to pick up new files.
     */
    fun refreshLibrary() {
        trackCache.clearCache()
        scanAndCacheLibrary()
    }
    
    /**
     * Rescan the library and look up again the artists that have no genre yet.
     * Genres already fetched from Last.fm are kept, so repeating this fills
     * in the gaps instead of starting over.
     */
    fun refreshGenres() {
        viewModelScope.launch {
            repository.forgetMissingGenres()
            trackCache.clearCache()
            scanAndCacheLibrary()
        }
    }
    
    /**
     * Scan folders and save to cache.
     */
    private fun scanAndCacheLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val tracks = repository.scanFolders(_uiState.value.selectedFolders)
            
            // Save to cache for next startup
            trackCache.saveTracks(tracks)
            
            showLibrary(tracks)
        }
    }

    /**
     * Show these tracks as the library, with the genres set by hand applied
     * on top of the automatic ones.
     */
    private fun showLibrary(tracks: List<Track>) {
        libraryTracks = tracks
        val manualGenres = genreOverrides.all()
        val shown = tracks.map { track ->
            GenreOverrides.manualGenre(manualGenres, track.id, track.artist)
                ?.let { track.copy(genre = it) }
                ?: track
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            tracks = shown,
            folders = repository.groupByFolder(shown),
            genres = repository.groupByGenre(shown),
            manualGenres = manualGenres
        )
    }

    /**
     * Set a track's genre by hand, for that track only or for every track by its artist.
     */
    fun setGenre(track: Track, genre: String, wholeArtist: Boolean) {
        if (wholeArtist) {
            val artist = ArtistParser.normalizeArtistName(track.artist)
            val artistTrackIds = libraryTracks
                .filter { ArtistParser.normalizeArtistName(it.artist) == artist }
                .map { it.id }
            genreOverrides.setForArtist(track.artist, genre, artistTrackIds)
        } else {
            genreOverrides.setForTrack(track.id, genre)
        }
        showLibrary(libraryTracks)
    }

    /**
     * Go back to the automatic genre for a track and its artist.
     */
    fun resetGenre(track: Track) {
        genreOverrides.clear(track.id, track.artist)
        showLibrary(libraryTracks)
    }

    fun playTrackList(tracks: List<Track>, startIndex: Int) {
        playbackManager.playTracks(tracks, startIndex)
        _uiState.value = _uiState.value.copy(
            nowPlaying = tracks.getOrNull(startIndex),
            playingQueue = tracks,
            isPlaying = true
        )
    }

    /**
     * Add tracks to the end of the current queue without disrupting playback
     */
    fun addToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        
        // If nothing is playing, start playing the first track
        if (_uiState.value.nowPlaying == null) {
            playTrackList(tracks, 0)
        } else {
            // Add to existing queue
            playbackManager.addToQueue(tracks)
            _uiState.value = _uiState.value.copy(
                playingQueue = _uiState.value.playingQueue + tracks
            )
        }
    }

    /**
     * Toggle play/pause for a specific track.
     * If the track is current, toggle play/pause.
     * If the track is different, play it.
     */
    fun togglePlayPauseTrack(tracks: List<Track>, trackIndex: Int) {
        val track = tracks.getOrNull(trackIndex) ?: return
        val currentTrack = _uiState.value.nowPlaying
        
        if (currentTrack?.id == track.id) {
            // Same track - toggle play/pause
            playPause()
        } else {
            // Different track - play it
            playTrackList(tracks, trackIndex)
        }
    }

    fun playPause() {
        playbackManager.playPause()
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    /** Position in the playing track, in milliseconds. */
    fun playbackPositionMs(): Long = playbackManager.currentPosition()

    /** Length of the playing track; from the file's metadata until the player knows it. */
    fun playbackDurationMs(): Long =
        playbackManager.duration().takeIf { it > 0L }
            ?: _uiState.value.nowPlaying?.durationMs
            ?: 0L

    /** Jump to a position in the playing track. */
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun next() = playbackManager.next()

    fun previous() = playbackManager.previous()

    override fun onCleared() {
        playbackManager.disconnect()
        super.onCleared()
    }
}
