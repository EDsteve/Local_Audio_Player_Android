# Local Audio Player

A modern Android music player app that plays local audio files from user-selected folders. Built with Kotlin, Jetpack Compose, and Media3 ExoPlayer.

![Android](https://img.shields.io/badge/Android-14%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple)

## Features

### 🎵 Core Features
- **Local Music Playback** - Play audio files from your device (no streaming)
- **Folder-based Browsing** - Select and browse music from specific folders
- **Hierarchical Navigation** - Navigate into subfolders with back navigation
- **Background Playback** - Continue playing music when the app is in background
- **Media Controls** - Play, pause, skip, previous, shuffle

### 📁 Folder Management
- **Select Multiple Folders** - Add multiple music folders to your library
- **Persistent Folders** - Selected folders are remembered across app restarts
- **Folder Play Button** - Play all tracks in a folder (including subfolders) with one tap
- **Instant Library Load** - Library is cached for fast startup

### 🎨 Modern UI
- **Material 3 Design** - Clean, modern interface following Material guidelines
- **Dark Theme** - Dark gray theme with subtle orange accents
- **Now Playing Bar** - Shows current track title and artist
- **Genre Categorization** - Auto-categorize tracks into 20 genre buckets

## Screenshots

<!-- Add screenshots here -->

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Playback:** Media3 ExoPlayer with MediaSession
- **Min SDK:** Android 14 (API 34)
- **Architecture:** MVVM with Unidirectional Data Flow

## Architecture

```
app/
├── data/
│   ├── MusicRepository.kt      # Folder scanning and track grouping
│   ├── GenreMapper.kt          # Genre classification
│   ├── PreferencesManager.kt   # Persistent folder storage
│   └── TrackCache.kt           # Track library caching
├── model/
│   └── Models.kt               # Track, Folder, GenreBucket data classes
├── playback/
│   ├── PlaybackService.kt      # Foreground service for media playback
│   └── PlaybackManager.kt      # MediaController wrapper
└── ui/
    ├── Theme.kt                # Material 3 theme configuration
    ├── PlayerViewModel.kt      # Main ViewModel
    ├── MainScaffold.kt         # App shell with navigation
    └── screens/
        ├── HomeScreen.kt       # Library overview
        ├── FolderScreen.kt     # Folder browser
        ├── GenreScreen.kt      # Genre browser
        └── NowPlayingBar.kt    # Playback controls
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Android device or emulator running Android 14+

### Build & Run

1. Clone the repository:
```bash
git clone https://github.com/EDsteve/Local_Audio_Player_Android.git
```

2. Open in Android Studio

3. Sync Gradle and build the project

4. Run on your device or emulator

### Permissions

The app uses the Storage Access Framework (SAF) to access music files, so no special storage permissions are required beyond folder selection.

## Usage

1. **First Launch:** Tap "Add Folder" to select a music folder
2. **Browse:** Use the bottom navigation to switch between Home, Folders, and Genres
3. **Play:** Tap any track to play, or tap the play button on a folder to play all tracks
4. **Navigate:** Tap a folder name to browse its contents, tap back to return
5. **Refresh:** Use the refresh button to scan for new files

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) for audio playback
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI
- [Material Design 3](https://m3.material.io/) for design guidelines
