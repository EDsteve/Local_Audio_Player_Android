# Local Audio Player

A modern Android music player app that plays local audio files from user-selected folders. Built with Kotlin, Jetpack Compose, and Media3 ExoPlayer.

![Version](https://img.shields.io/badge/version-0.2-orange)
![Status](https://img.shields.io/badge/status-Early%20Development-yellow)
![Android](https://img.shields.io/badge/Android-14%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple)

> ⚠️ **Early Development Notice:** This app is currently in v0.2 and under active development. Some features may be incomplete or missing. Contributions and feedback are welcome!

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
- **Search Functionality** - Search tracks by title, artist, or album on all screens

### 🏷️ Enhanced Genre Detection (NEW in v0.2!)
- **Last.fm API Integration** - Automatic genre detection using Last.fm's extensive music database
- **Artist-based Genre Tagging** - Looks up artist genres for accurate categorization
- **20 Genre Categories** - Tracks are organized into: Rock, Pop, Hip-Hop, Electronic, R&B, Jazz, Blues, Classical, Country, Folk, Latin, Reggae, Metal, Ambient, Soundtrack, Gospel, Children, Spoken Word, World, and Other
- **Sub-genres** - Each category holds its sub-genres like folders: Rock contains Alternative Rock, Indie Rock, Punk and so on. Playing Rock plays all of them
- **Smart Caching** - Genre lookups are cached locally (Room database) for instant subsequent scans
- **Offline Support** - Works offline for previously scanned artists
- **Refresh Button** - Re-scan genres with updated mappings when needed

## ⚠️ Known Limitations (v0.2)

These features are **not yet implemented** or have known issues:

| Feature | Status |
|---------|--------|
| 🔘 Playback progress/seek bar | Not implemented |
| 🖼️ Album art display | Not implemented |
| 📋 Playlist/queue management UI | Not implemented |
| ⚙️ Settings screen | Not implemented |
| 🎛️ Equalizer | Not implemented |
| 🏷️ Genre detection | ✅ Implemented via Last.fm API |

## 🚀 Roadmap / Future Improvements

### Near-term Goals
- [ ] **Playback Progress Bar** - Visual seek bar with drag-to-seek
- [ ] **Album Art Display** - Show embedded album artwork
- [ ] **Queue Management UI** - View and manage playback queue

### Medium-term Goals
- [ ] **Settings Screen** - Playback preferences, theme options
- [ ] **Enhanced Notification Controls** - Rich media notification with album art
- [ ] **Toast/Snackbar Feedback** - Visual feedback for queue operations

### Long-term Goals
- [ ] **Playlist Support** - Create and manage playlists
- [ ] **Equalizer Integration** - Audio equalizer with presets
- [ ] **Sleep Timer** - Auto-stop playback after set time
- [ ] **Android Auto Support** - Car integration

## Screenshots

<!-- Add screenshots here -->

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Playback:** Media3 ExoPlayer with MediaSession
- **Database:** Room (for genre caching)
- **Networking:** Retrofit + OkHttp (for Last.fm API)
- **Min SDK:** Android 14 (API 34)
- **Architecture:** MVVM with Unidirectional Data Flow

## Architecture

```
app/
├── data/
│   ├── MusicRepository.kt      # Folder scanning and track grouping
│   ├── GenreMapper.kt          # Genre tree: sub-genre -> top genre
│   ├── GenreEnhancer.kt        # Last.fm API + cache orchestration
│   ├── ArtistParser.kt         # Extract artist from filename/path
│   ├── PreferencesManager.kt   # Persistent folder storage
│   ├── TrackCache.kt           # Track library caching
│   ├── cache/
│   │   ├── GenreDatabase.kt    # Room database
│   │   ├── ArtistGenreCache.kt # Cache entity
│   │   └── ArtistGenreCacheDao.kt # Cache DAO
│   └── lastfm/
│       ├── LastFmApi.kt        # Retrofit API interface
│       └── LastFmModels.kt     # API response models
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
        ├── GenreScreen.kt      # Genre browser with refresh button
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

The app uses:
- **Storage Access Framework (SAF)** - For accessing music files (no storage permissions needed)
- **Internet** - For Last.fm API genre lookups (optional, works offline with cache)

## Usage

1. **First Launch:** Tap "Add Folder" to select a music folder
2. **Browse:** Use the bottom navigation to switch between Home, Folders, and Genres
3. **Play:** Tap any track to play, or tap the play button on a folder to play all tracks
4. **Navigate:** Tap a folder name to browse its contents, tap back to return
5. **Search:** Use the search bar on any screen to find tracks
6. **Refresh Genres:** Tap the refresh button (🔄) in the Genres screen to re-scan with updated mappings

### Genre Detection

The app automatically detects genres using the Last.fm API:
- On first scan, each unique artist is looked up on Last.fm
- The first tag that is a known genre is kept as the track's sub-genre (e.g. Indie Rock) and filed under one of 20 genre categories (Rock)
- Results are cached locally for instant subsequent scans
- If genres seem incorrect, use the refresh button to re-scan

## Contributing

Contributions are welcome! This project is in early development, so there's plenty of opportunity to help shape its direction.

### How to Contribute
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Areas Where Help is Needed
- UI/UX improvements
- Playback seek bar implementation
- Album art extraction and display
- Testing on various Android devices

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) for audio playback
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI
- [Material Design 3](https://m3.material.io/) for design guidelines
- [Last.fm API](https://www.last.fm/api) for genre/tag data
- [Room Database](https://developer.android.com/training/data-storage/room) for local caching
