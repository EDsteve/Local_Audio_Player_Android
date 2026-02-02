package com.localplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.localplayer.model.GenreBucket
import com.localplayer.model.Track
import com.localplayer.ui.PlayerUiState

@Composable
fun GenreScreen(
    uiState: PlayerUiState,
    onPlayGenre: (List<Track>, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Genres", style = MaterialTheme.typography.titleLarge) }
        items(uiState.genres) { genre ->
            GenreCard(genre = genre, onPlayGenre = onPlayGenre)
        }
    }
}

@Composable
private fun GenreCard(genre: GenreBucket, onPlayGenre: (List<Track>, Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlayGenre(genre.tracks, 0) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Category, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(genre.name, style = MaterialTheme.typography.titleSmall)
                Text("${genre.tracks.size} tracks", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
