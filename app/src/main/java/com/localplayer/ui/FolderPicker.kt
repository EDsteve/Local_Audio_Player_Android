package com.localplayer.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberFolderPicker(onFolderPicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                persistUriPermission(context, uri)
                onFolderPicked(uri)
            } catch (e: Exception) {
                Log.e("FolderPicker", "Error handling folder selection", e)
                // Still try to call the callback even if permission persistence fails
                onFolderPicked(uri)
            }
        }
    }

    return {
        launcher.launch(null)
    }
}

private fun persistUriPermission(context: Context, uri: Uri) {
    try {
        // Only request READ permission as we don't need to write
        val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
    } catch (e: SecurityException) {
        Log.e("FolderPicker", "Failed to persist URI permission", e)
        // This is not fatal - the app can still read the folder for this session
    }
}
