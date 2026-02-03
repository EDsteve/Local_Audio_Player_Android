package com.localplayer.data

/**
 * Utility for extracting artist names from filenames and metadata.
 * Uses common naming patterns to parse artist when metadata is missing.
 */
object ArtistParser {
    
    // Common filename patterns for music files
    // Pattern: "Artist - Title.ext"
    private val artistTitlePattern = Regex("^(.+?)\\s*-\\s*(.+)$")
    
    // Pattern: "01 - Artist - Title.ext" or "01. Artist - Title.ext"
    private val trackArtistTitlePattern = Regex("^\\d+[.\\-\\s]+(.+?)\\s*-\\s*(.+)$")
    
    // Pattern: "Artist_-_Title.ext" or "Artist - Title.ext" (underscores)
    private val underscorePattern = Regex("^(.+?)_-_(.+)$")
    
    /**
     * Extract artist from filename.
     * Tries various common naming patterns.
     * 
     * @param filename The filename without extension
     * @return Extracted artist name or null if no pattern matches
     */
    fun extractArtistFromFilename(filename: String): String? {
        val cleanName = filename
            .substringBeforeLast(".") // Remove extension if present
            .trim()
        
        // Try track number + artist + title pattern first (e.g., "01 - Coldplay - Yellow")
        trackArtistTitlePattern.find(cleanName)?.let { match ->
            val artist = match.groupValues[1].cleanArtistName()
            if (artist.isNotBlank() && isValidArtistName(artist)) {
                return artist
            }
        }
        
        // Try underscore pattern (e.g., "Coldplay_-_Yellow")
        underscorePattern.find(cleanName)?.let { match ->
            val artist = match.groupValues[1].cleanArtistName()
            if (artist.isNotBlank() && isValidArtistName(artist)) {
                return artist
            }
        }
        
        // Try basic artist - title pattern (e.g., "Coldplay - Yellow")
        artistTitlePattern.find(cleanName)?.let { match ->
            val artist = match.groupValues[1].cleanArtistName()
            if (artist.isNotBlank() && isValidArtistName(artist)) {
                return artist
            }
        }
        
        return null
    }
    
    /**
     * Extract artist from folder path.
     * Assumes folder structure like "Artist/Album/Track.mp3" or "Artist/Track.mp3"
     * 
     * @param folderPath The relative folder path
     * @return Extracted artist name or null
     */
    fun extractArtistFromFolderPath(folderPath: String): String? {
        if (folderPath.isBlank()) return null
        
        val parts = folderPath.split("/").filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        
        // First folder is often the artist
        val potentialArtist = parts.first().cleanArtistName()
        if (potentialArtist.isNotBlank() && isValidArtistName(potentialArtist)) {
            return potentialArtist
        }
        
        return null
    }
    
    /**
     * Normalize artist name for cache lookup.
     * Ensures consistent formatting for cache key.
     */
    fun normalizeArtistName(artist: String): String {
        return artist
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace("&", "and")
    }
    
    /**
     * Clean artist name from filename artifacts.
     */
    private fun String.cleanArtistName(): String {
        return this
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Check if extracted name is likely a valid artist name.
     * Filters out common non-artist patterns.
     */
    private fun isValidArtistName(name: String): Boolean {
        val normalized = name.lowercase()
        
        // Filter out common non-artist patterns
        val invalidPatterns = listOf(
            "track", "unknown", "various", "va", "compilation",
            "soundtrack", "ost", "original", "audio", "music",
            "untitled", "none", "n/a", "null"
        )
        
        // Must be at least 2 characters
        if (normalized.length < 2) return false
        
        // Must not be purely numeric
        if (normalized.all { it.isDigit() }) return false
        
        // Must not be a known invalid pattern
        if (invalidPatterns.any { normalized == it || normalized.startsWith("$it ") }) {
            return false
        }
        
        return true
    }
    
    /**
     * Get the best available artist from multiple sources.
     * Priority: metadata > filename > folder path
     */
    fun getBestArtist(
        metadataArtist: String?,
        filename: String,
        folderPath: String
    ): String {
        // First try metadata
        if (!metadataArtist.isNullOrBlank() && 
            metadataArtist.lowercase() != "unknown" &&
            isValidArtistName(metadataArtist)) {
            return metadataArtist
        }
        
        // Then try filename
        extractArtistFromFilename(filename)?.let { return it }
        
        // Then try folder path
        extractArtistFromFolderPath(folderPath)?.let { return it }
        
        // Return original metadata artist or "Unknown"
        return metadataArtist ?: "Unknown"
    }
}
