package com.localplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localplayer.data.ArtistParser
import com.localplayer.data.GenreMapper
import com.localplayer.data.GenreOverrides
import com.localplayer.model.Track
import com.localplayer.ui.PlayerUiState

/**
 * Lets the user pick the genre of a track, or of every track by its artist,
 * when the automatic one is wrong or missing. Tapping a genre applies it.
 *
 * @param onSetGenre track, chosen genre, and whether it applies to the whole artist
 * @param onResetGenre go back to the automatic genre
 */
@Composable
fun SetGenreDialog(
    track: Track,
    uiState: PlayerUiState,
    onSetGenre: (Track, String, Boolean) -> Unit,
    onResetGenre: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    // A genre for the whole artist only makes sense if we know who the artist is
    val artistKnown = track.artist.isNotBlank() && !track.artist.equals("Unknown", ignoreCase = true)
    val artistKey = ArtistParser.normalizeArtistName(track.artist)
    val artistTrackCount = uiState.tracks.count { ArtistParser.normalizeArtistName(it.artist) == artistKey }
    var wholeArtist by remember { mutableStateOf(artistKnown) }

    val isSetByHand = GenreOverrides.manualGenre(uiState.manualGenres, track.id, track.artist) != null

    // Top genres until the user types; then everything that matches
    val choices = if (query.isBlank()) {
        GenreMapper.choices.filter { it.isTopGenre }
    } else {
        GenreMapper.choices.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set genre") },
        text = {
            Column {
                Text(
                    "${track.title} · ${track.artist}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Now in ${genrePath(track.genre)}" + if (isSetByHand) " (set by you)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (artistKnown) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { wholeArtist = !wholeArtist },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = wholeArtist, onCheckedChange = { wholeArtist = it })
                        Text(
                            "All ${countLabel(artistTrackCount, "track")} by ${track.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search sub-genres...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(choices) { choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetGenre(track, choice.name, artistKnown && wholeArtist)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                choice.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (!choice.isTopGenre) {
                                Text(
                                    "in ${choice.topGenre}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (choices.isEmpty()) {
                        item {
                            Text(
                                "No genre matches \"${query.trim()}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        dismissButton = if (isSetByHand) {
            {
                TextButton(onClick = {
                    onResetGenre(track)
                    onDismiss()
                }) { Text("Use automatic") }
            }
        } else {
            null
        }
    )
}

/** "Rock", or "Rock › Indie Rock" for a sub-genre. */
private fun genrePath(genre: String): String {
    val top = GenreMapper.parentOf(genre)
    val name = GenreMapper.displayName(genre)
    return if (name == top) top else "$top › $name"
}
