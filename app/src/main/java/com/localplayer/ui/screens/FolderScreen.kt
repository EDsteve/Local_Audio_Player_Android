package com.localplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    onPlayFolder: (List<Track>, Int) -> Unit
) {
    // Navigation stack: list of folders we've navigated into
    var navigationStack by remember { mutableStateOf<List<Folder>>(emptyList()) }

    // Current folder contents to display
    val currentFolders = if (navigationStack.isEmpty()) {
        uiState.folders
    } else {
        navigationStack.last().subfolders
    }

    val currentTracks = if (navigationStack.isEmpty()) {
        emptyList()
    } else {
        navigationStack.last().tracks
    }

    val currentFolderName = navigationStack.lastOrNull()?.name

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with back button if navigated into a folder
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (navigationStack.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            navigationStack = navigationStack.dropLast(1)
                        }
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

        // Subfolders
        itemsIndexed(currentFolders) { _, folder ->
            FolderCard(
                folder = folder,
                onPlayFolder = { onPlayFolder(folder.allTracksRecursive(), 0) },
                onNavigateInto = {
                    navigationStack = navigationStack + folder
                }
            )
        }

        // Tracks in current folder
        itemsIndexed(currentTracks) { index, track ->
            TrackCard(
                track = track,
                onClick = { onPlayFolder(currentTracks, index) }
            )
        }
    }
}

@Composable
private fun FolderCard(
    folder: Folder,
    onPlayFolder: () -> Unit,
    onNavigateInto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder content - clickable to navigate
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateInto() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(folder.name, style = MaterialTheme.typography.titleSmall)
                    val totalTracks = folder.totalTrackCount()
                    val subfoldersCount = folder.subfolders.size
                    val description = buildString {
                        append("$totalTracks tracks")
                        if (subfoldersCount > 0) {
                            append(" • $subfoldersCount subfolders")
                        }
                    }
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Play button on the right
            IconButton(
                onClick = onPlayFolder,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play folder",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
