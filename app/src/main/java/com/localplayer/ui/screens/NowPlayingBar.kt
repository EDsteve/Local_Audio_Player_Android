package com.localplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// How often the seek bar catches up with playback
private const val POSITION_REFRESH_MS = 500L

/**
 * @param trackId identifies the playing track, so the seek bar starts over when it changes
 * @param getPositionMs current playback position; asked twice a second while playing
 * @param getDurationMs length of the playing track, 0 if not known yet
 * @param onSeek jump to a position in the track
 */
@Composable
fun NowPlayingBar(
    trackId: String,
    title: String,
    artist: String,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    getPositionMs: () -> Long,
    getDurationMs: () -> Long,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            SeekBar(
                trackId = trackId,
                isPlaying = isPlaying,
                getPositionMs = getPositionMs,
                getDurationMs = getDurationMs,
                onSeek = onSeek
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                PlayerBarButton(
                    onClick = onShuffleToggle,
                    icon = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    iconSize = 24.dp,
                    buttonSize = 44.dp
                )
                PlayerBarButton(
                    onClick = onPrevious,
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    iconSize = 28.dp,
                    buttonSize = 46.dp
                )
                PlayerBarButton(
                    onClick = onPlayPause,
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    iconSize = 30.dp,
                    buttonSize = 50.dp
                )
                PlayerBarButton(
                    onClick = onNext,
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    iconSize = 28.dp,
                    buttonSize = 46.dp
                )
            }
        }
    }
}

/**
 * Elapsed time, a slider to move through the track, and the track's length.
 */
@Composable
private fun SeekBar(
    trackId: String,
    isPlaying: Boolean,
    getPositionMs: () -> Long,
    getDurationMs: () -> Long,
    onSeek: (Long) -> Unit
) {
    var positionMs by remember(trackId) { mutableStateOf(0L) }
    var durationMs by remember(trackId) { mutableStateOf(0L) }
    // Where the thumb is while the user drags it; playback only jumps on release
    var dragPositionMs by remember(trackId) { mutableStateOf<Long?>(null) }

    // Follow playback. While paused the position does not move, so one read is enough.
    LaunchedEffect(trackId, isPlaying) {
        while (true) {
            positionMs = getPositionMs()
            durationMs = getDurationMs()
            if (!isPlaying) break
            delay(POSITION_REFRESH_MS)
        }
    }

    val shownMs = (dragPositionMs ?: positionMs).coerceIn(0L, durationMs.coerceAtLeast(0L))

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(shownMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Slider(
            value = shownMs.toFloat(),
            onValueChange = { dragPositionMs = it.toLong() },
            onValueChangeFinished = {
                dragPositionMs?.let { target ->
                    onSeek(target)
                    positionMs = target
                }
                dragPositionMs = null
            },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            enabled = durationMs > 0L,
            modifier = Modifier.weight(1f).height(32.dp).padding(horizontal = 8.dp)
        )
        Text(
            text = formatTime(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/** 83000 -> "1:23", 3723000 -> "1:02:03" */
private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun PlayerBarButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    iconSize: Dp,
    buttonSize: Dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 2.dp).size(buttonSize)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}
