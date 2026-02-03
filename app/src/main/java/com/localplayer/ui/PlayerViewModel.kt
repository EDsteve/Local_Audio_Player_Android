package com.localplayer.ui

import android.app.Application
import android.net.Uri
import androidx.media3.common.Player
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
                _uiState.value = _uiState.value.copy(
                    tracks = cachedTracks,
                    folders = repository.groupByFolder(cachedTracks),
                    genres = repository.groupByGenre(cachedTracks)
                )
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
     * Clear genre cache and rescan library.
     * Use this when genres are incorrectly categorized and user wants to refresh.
     */
    fun clearGenreCacheAndRescan() {
        viewModelScope.launch {
            repository.clearGenreCache()
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
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                tracks = tracks,
                folders = repository.groupByFolder(tracks),
                genres = repository.groupByGenre(tracks)
            )
        }
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

    fun next() = playbackManager.next()

    fun previous() = playbackManager.previous()

    override fun onCleared() {
        playbackManager.disconnect()
        super.onCleared()
    }
}
