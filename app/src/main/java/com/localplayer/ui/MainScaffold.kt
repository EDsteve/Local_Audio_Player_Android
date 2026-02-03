package com.localplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localplayer.ui.screens.FolderScreen
import com.localplayer.ui.screens.GenreScreen
import com.localplayer.ui.screens.HomeScreen
import com.localplayer.ui.screens.NowPlayingBar

private enum class TabItem { Home, Folders, Genres }

@Composable
fun MainScaffold(viewModel: PlayerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(TabItem.Home) }

    val pickFolder = rememberFolderPicker { uri ->
        val updated = uiState.selectedFolders + uri
        viewModel.setSelectedFolders(updated.distinct())
    }

    Scaffold(
        bottomBar = {
            Column {
                uiState.nowPlaying?.let { track ->
                    NowPlayingBar(
                        title = track.title,
                        artist = track.artist,
                        isPlaying = uiState.isPlaying,
                        isShuffleEnabled = uiState.isShuffleEnabled,
                        onShuffleToggle = viewModel::toggleShuffle,
                        onPrevious = viewModel::previous,
                        onPlayPause = viewModel::playPause,
                        onNext = viewModel::next
                    )
                }
                NavigationBar(
                    containerColor = Color.Black
                ) {
                    NavigationBarItem(
                        selected = currentTab == TabItem.Home,
                        onClick = { currentTab = TabItem.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentTab == TabItem.Folders,
                        onClick = { currentTab = TabItem.Folders },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        label = { Text("Folders") }
                    )
                    NavigationBarItem(
                        selected = currentTab == TabItem.Genres,
                        onClick = { currentTab = TabItem.Genres },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        label = { Text("Genres") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                TabItem.Home -> HomeScreen(
                    uiState = uiState,
                    onPickFolder = pickFolder,
                    onRefresh = viewModel::refreshLibrary,
                    onPlayTrack = viewModel::playTrackList,
                    onRemoveFolder = viewModel::removeFolder,
                    onTogglePlayPause = viewModel::togglePlayPauseTrack
                )
                TabItem.Folders -> FolderScreen(
                    uiState = uiState,
                    onPlayFolder = viewModel::playTrackList,
                    onAddToQueue = viewModel::addToQueue,
                    onTogglePlayPause = viewModel::togglePlayPauseTrack
                )
                TabItem.Genres -> GenreScreen(
                    uiState = uiState,
                    onPlayGenre = viewModel::playTrackList,
                    onAddToQueue = viewModel::addToQueue,
                    onTogglePlayPause = viewModel::togglePlayPauseTrack
                )
            }
        }
    }
}
