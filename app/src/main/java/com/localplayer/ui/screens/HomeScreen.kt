package com.localplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localplayer.model.Track
import com.localplayer.ui.PlayerUiState

@Composable
fun HomeScreen(
    uiState: PlayerUiState,
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onPlayTrack: (List<Track>, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("LocalPlayer", style = MaterialTheme.typography.headlineMedium)
            Text("Choose folders and start listening", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selected folders", style = MaterialTheme.typography.titleMedium)
                    if (uiState.selectedFolders.isEmpty()) {
                        Text("No folders selected yet")
                    } else {
                        uiState.selectedFolders.forEach { uri ->
                            Text(uri.lastPathSegment ?: uri.toString(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPickFolder) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add folder")
                        }
                        Button(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan")
                        }
                    }
                }
            }
        }

        if (uiState.tracks.isNotEmpty()) {
            item {
                Text("All Tracks", style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(uiState.tracks) { index, track ->
                TrackRow(track = track, onPlay = { onPlayTrack(uiState.tracks, index) })
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onPlay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlay() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall)
                Text("${track.artist} · ${track.album}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
