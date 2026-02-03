package com.localplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localplayer.model.GenreBucket
import com.localplayer.model.Track
import com.localplayer.ui.PlayerUiState

// Genre colors for visual distinction
object GenreColors {
    val Pop = Color(0xFFFF6B6B)
    val Rock = Color(0xFF4ECDC4)
    val Jazz = Color(0xFFFFE66D)
    val Classical = Color(0xFFA8E6CF)
    val Electronic = Color(0xFF95E1D3)
    val HipHop = Color(0xFFF38181)
    val RnB = Color(0xFFAA96DA)
    val Metal = Color(0xFF3D5A80)
    val Country = Color(0xFFE8A87C)
    val Blues = Color(0xFF5AA9E6)
    val Reggae = Color(0xFF55D6BE)
    val Latin = Color(0xFFFC8A4D)
    val Folk = Color(0xFF8B9A46)
    val Soul = Color(0xFFE07BE0)
    val Punk = Color(0xFFFF5E5B)
    val Indie = Color(0xFFD4A5A5)
    val Dance = Color(0xFFB388EB)
    val World = Color(0xFF7FDBDA)
    val Soundtrack = Color(0xFF6C5B7B)
    val Other = Color(0xFFB8B8B8)
}

@Composable
fun GenreScreen(
    uiState: PlayerUiState,
    onPlayGenre: (List<Track>, Int) -> Unit,
    onAddToQueue: ((List<Track>) -> Unit)? = null,
    onTogglePlayPause: ((List<Track>, Int) -> Unit)? = null
) {
    // Search query
    var searchQuery by remember { mutableStateOf("") }
    
    // Selected genre for detailed view
    var selectedGenre by remember { mutableStateOf<GenreBucket?>(null) }
    
    // Handle back button
    BackHandler(enabled = selectedGenre != null) {
        selectedGenre = null
    }

    // Filter based on search
    val filteredTracks = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        uiState.tracks.filter { track ->
            track.title.contains(searchQuery, ignoreCase = true) ||
            track.artist.contains(searchQuery, ignoreCase = true) ||
            track.album.contains(searchQuery, ignoreCase = true)
        }
    }
    
    // Filter genres that have tracks
    val genresWithTracks = uiState.genres.filter { it.tracks.isNotEmpty() }

    // Loading state with animation
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning library...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header with back button if in genre detail view
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedGenre != null) {
                    IconButton(
                        onClick = { selectedGenre = null }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = selectedGenre?.name ?: "Genres",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tracks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // When searching, show track results
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    "${filteredTracks.size} tracks found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            itemsIndexed(filteredTracks) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = uiState.nowPlaying?.id == track.id && uiState.isPlaying,
                    isCurrentTrack = uiState.nowPlaying?.id == track.id,
                    onClick = {
                        if (onTogglePlayPause != null) {
                            onTogglePlayPause(filteredTracks, index)
                        } else {
                            onPlayGenre(filteredTracks, index)
                        }
                    },
                    onAddToQueue = { onAddToQueue?.invoke(listOf(track)) }
                )
            }
        } else if (selectedGenre != null) {
            // Show tracks for selected genre
            itemsIndexed(selectedGenre!!.tracks) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = uiState.nowPlaying?.id == track.id && uiState.isPlaying,
                    isCurrentTrack = uiState.nowPlaying?.id == track.id,
                    onClick = {
                        if (onTogglePlayPause != null) {
                            onTogglePlayPause(selectedGenre!!.tracks, index)
                        } else {
                            onPlayGenre(selectedGenre!!.tracks, index)
                        }
                    },
                    onAddToQueue = { onAddToQueue?.invoke(listOf(track)) }
                )
            }
        } else {
            // Show genres list with colorful icons
            items(genresWithTracks) { genre ->
                GenreRow(
                    genre = genre,
                    onPlayGenre = { onPlayGenre(genre.tracks, 0) },
                    onAddToQueue = { onAddToQueue?.invoke(genre.tracks) },
                    onOpenGenre = { selectedGenre = genre }
                )
            }
            
            // Empty state
            if (genresWithTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No genres yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add some music to see genre categories",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreRow(
    genre: GenreBucket,
    onPlayGenre: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenGenre: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val genreColor = getGenreColor(genre.name)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenGenre() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colorful genre icon
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = genreColor
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Genre info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                genre.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${genre.tracks.size} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Play button
        IconButton(
            onClick = onPlayGenre,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play genre",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Chevron / navigate icon
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "View genre",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Music note icon (static)
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = if (isCurrentTrack)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentTrack)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Play/Pause button (single button on the right)
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // More options
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    modifier = Modifier.size(24.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    onClick = {
                        onAddToQueue()
                        showMenu = false
                    }
                )
            }
        }
    }
}

/**
 * Returns a color associated with a genre for visual distinction.
 */
private fun getGenreColor(genre: String): Color {
    return when (genre.lowercase()) {
        "pop" -> GenreColors.Pop
        "rock" -> GenreColors.Rock
        "jazz" -> GenreColors.Jazz
        "classical" -> GenreColors.Classical
        "electronic" -> GenreColors.Electronic
        "hip-hop" -> GenreColors.HipHop
        "r&b" -> GenreColors.RnB
        "metal" -> GenreColors.Metal
        "country" -> GenreColors.Country
        "blues" -> GenreColors.Blues
        "reggae" -> GenreColors.Reggae
        "latin" -> GenreColors.Latin
        "folk" -> GenreColors.Folk
        "soul" -> GenreColors.Soul
        "punk" -> GenreColors.Punk
        "indie" -> GenreColors.Indie
        "dance" -> GenreColors.Dance
        "world" -> GenreColors.World
        "soundtrack" -> GenreColors.Soundtrack
        else -> GenreColors.Other
    }
}
