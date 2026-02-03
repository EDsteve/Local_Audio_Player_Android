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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localplayer.model.Folder
import com.localplayer.model.Track
import com.localplayer.ui.PlayerUiState

@Composable
fun FolderScreen(
    uiState: PlayerUiState,
    onPlayFolder: (List<Track>, Int) -> Unit,
    onAddToQueue: (List<Track>) -> Unit,
    onTogglePlayPause: (List<Track>, Int) -> Unit
) {
    // Search query
    var searchQuery by remember { mutableStateOf("") }
    
    // Navigation stack for navigating into folders
    var navigationStack by remember { mutableStateOf<List<Folder>>(emptyList()) }
    
    // Handle back button
    BackHandler(enabled = navigationStack.isNotEmpty()) {
        navigationStack = navigationStack.dropLast(1)
    }

    // Current folder being viewed (null = root level)
    val currentFolder = navigationStack.lastOrNull()
    val currentFolderName = currentFolder?.name
    
    // Determine what to display
    val displayFolders: List<Folder>
    val displayTracks: List<Track>
    
    when {
        searchQuery.isNotBlank() -> {
            // Search mode: show matching tracks
            displayFolders = emptyList()
            displayTracks = uiState.tracks.filter { track ->
                track.title.contains(searchQuery, ignoreCase = true) ||
                track.artist.contains(searchQuery, ignoreCase = true) ||
                track.album.contains(searchQuery, ignoreCase = true)
            }
        }
        navigationStack.isNotEmpty() -> {
            // Inside a folder: show its subfolders and tracks
            displayFolders = currentFolder!!.subfolders
            displayTracks = currentFolder.tracks
        }
        else -> {
            // Root level: show root subfolders and tracks with empty folderPath
            displayFolders = uiState.folders
            // Tracks at root level have empty folderPath ("")
            displayTracks = uiState.tracks.filter { it.folderPath.isEmpty() }
        }
    }

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
        // Header with back button if navigated into a folder
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (navigationStack.isNotEmpty()) {
                    IconButton(
                        onClick = { navigationStack = navigationStack.dropLast(1) }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = currentFolderName ?: "Folders",
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

        // Display search results count when searching
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    "${displayTracks.size} tracks found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Subfolders list
        itemsIndexed(displayFolders) { _, folder ->
            FolderRow(
                folder = folder,
                onPlayFolder = { onPlayFolder(folder.allTracksRecursive(), 0) },
                onAddToQueue = { onAddToQueue(folder.allTracksRecursive()) },
                onOpenFolder = { 
                    navigationStack = navigationStack + folder 
                }
            )
        }
        
        // Divider between folders and tracks
        if (displayFolders.isNotEmpty() && displayTracks.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        // Tracks list
        itemsIndexed(displayTracks) { index, track ->
            TrackRow(
                track = track,
                isPlaying = uiState.nowPlaying?.id == track.id && uiState.isPlaying,
                isCurrentTrack = uiState.nowPlaying?.id == track.id,
                onClick = { onTogglePlayPause(displayTracks, index) },
                onAddToQueue = { onAddToQueue(listOf(track)) }
            )
        }
        
        // Empty state
        if (displayFolders.isEmpty() && displayTracks.isEmpty() && searchQuery.isBlank()) {
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
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No music found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add a folder in the Home tab to see your music",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: Folder,
    onPlayFolder: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenFolder: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFolder() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Folder icon
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Folder info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                folder.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${folder.totalTrackCount()} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Play button
        IconButton(
            onClick = onPlayFolder,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play folder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Three-dot menu for Add to Queue
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
                formatDuration(track.durationMs),
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

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
