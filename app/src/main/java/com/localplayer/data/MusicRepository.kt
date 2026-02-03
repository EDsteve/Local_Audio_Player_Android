package com.localplayer.data

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.localplayer.model.Folder
import com.localplayer.model.GenreBucket
import com.localplayer.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun scanFolders(folderUris: List<Uri>): List<Track> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val tracks = mutableListOf<Track>()
        folderUris.forEach { treeUri ->
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootName = rootDocId.substringAfterLast("/").substringAfterLast(":")
            // Start scanning from root with empty relative path (tracks in root have "" path)
            scanFolder(resolver, treeUri, rootDocId, tracks, "")
        }
        tracks
    }

    private fun scanFolder(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        tracks: MutableList<Track>,
        currentRelativePath: String
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val docId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex)
                val mime = cursor.getString(mimeIndex)
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                if (DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                    // Build the new relative path for this subfolder
                    val newRelativePath = if (currentRelativePath.isEmpty()) name else "$currentRelativePath/$name"
                    scanFolder(resolver, treeUri, docId, tracks, newRelativePath)
                } else if (mime.startsWith("audio")) {
                    // Use the current relative path for the track's folder
                    tracks.add(extractTrack(docUri, name, currentRelativePath, resolver))
                }
            }
        }
    }

    private fun extractTrack(
        uri: Uri,
        name: String,
        folderName: String,
        resolver: ContentResolver
    ): Track {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown"
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown"
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val genreRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
        val genre = GenreMapper.mapToTopGenre(genreRaw)
        retriever.release()
        return Track(
            id = uri.toString(),
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            contentUri = uri,
            folderPath = folderName,
            genre = genre
        )
    }

    /**
     * Creates a single root folder containing all content.
     * Returns a Folder with subfolders and tracks at root level.
     */
    fun createRootFolder(tracks: List<Track>): Folder? {
        if (tracks.isEmpty()) return null
        
        val tree = buildFolderTree(tracks)
        
        // Create a virtual root folder that contains all the subfolders
        // and any tracks that are at the "root" level (tracks not in nested folders)
        return Folder(
            path = "root",
            name = "Music",
            tracks = emptyList(), // root level tracks will be in the first folder level
            subfolders = tree
        )
    }

    fun groupByFolder(tracks: List<Track>): List<Folder> {
        return buildFolderTree(tracks)
    }

    /**
     * Builds a hierarchical folder tree from flat track list.
     * Groups tracks by their folder paths and creates nested Folder structure.
     */
    private fun buildFolderTree(tracks: List<Track>): List<Folder> {
        // Group tracks by their folder path
        val tracksByPath = tracks.groupBy { it.folderPath }

        // Build the tree structure
        val rootFolders = mutableMapOf<String, MutableList<Track>>()
        val childPaths = mutableMapOf<String, MutableSet<String>>()

        // Collect all unique folder paths and determine parent-child relationships
        val allPaths = tracksByPath.keys.toList()

        for (path in allPaths) {
            // Find if this path has a parent among other paths
            var foundParent = false
            for (potentialParent in allPaths) {
                if (potentialParent != path && path.startsWith("$potentialParent/")) {
                    // This path is a child of potentialParent
                    childPaths.getOrPut(potentialParent) { mutableSetOf() }.add(path)
                    foundParent = true
                    break
                }
            }

            if (!foundParent) {
                // This is a root-level folder
                rootFolders[path] = tracksByPath[path]?.toMutableList() ?: mutableListOf()
            }
        }

        // Build folders recursively
        fun buildFolder(path: String): Folder {
            val directTracks = tracksByPath[path] ?: emptyList()
            val children = childPaths[path] ?: emptySet()

            // Get only direct children (not nested children)
            val directChildren = children.filter { childPath ->
                // A direct child should not have any intermediate path between parent and child
                val relativePath = childPath.removePrefix("$path/")
                !relativePath.contains("/")
            }.toSet()

            // Indirect children go deeper
            val indirectChildren = children - directChildren

            // Add indirect children to their direct parent
            for (indirect in indirectChildren) {
                val relativePath = indirect.removePrefix("$path/")
                val directChildName = relativePath.substringBefore("/")
                val directChildPath = "$path/$directChildName"
                if (directChildPath in directChildren) {
                    childPaths.getOrPut(directChildPath) { mutableSetOf() }.add(indirect)
                }
            }

            val subfolders = directChildren.map { childPath ->
                buildFolder(childPath)
            }.sortedBy { it.name.lowercase() }

            return Folder(
                path = path,
                name = path.substringAfterLast("/").ifBlank { path },
                tracks = directTracks,
                subfolders = subfolders
            )
        }

        return rootFolders.keys.map { rootPath ->
            buildFolder(rootPath)
        }.sortedBy { it.name.lowercase() }
    }

    fun groupByGenre(tracks: List<Track>): List<GenreBucket> {
        val bucketMap = GenreMapper.topGenres.associateWith { mutableListOf<Track>() }
        tracks.forEach { track ->
            val list = bucketMap[track.genre] ?: bucketMap.getValue("Other")
            list.add(track)
        }
        return bucketMap.map { (name, items) ->
            GenreBucket(name = name, tracks = items)
        }
    }
}
