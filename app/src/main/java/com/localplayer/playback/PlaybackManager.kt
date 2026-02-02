package com.localplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.localplayer.model.Track

class PlaybackManager(private val context: Context) {
    private var controller: MediaController? = null
    private var controllerListener: Player.Listener? = null

    fun connect(onReady: (MediaController) -> Unit = {}) {
        if (controller != null) {
            controller?.let { onReady(it) }
            return
        }
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controllerListener?.let { controller?.addListener(it) }
            controller?.let { onReady(it) }
        }, { runnable -> runnable.run() })
    }

    fun disconnect() {
        controllerListener?.let { listener ->
            controller?.removeListener(listener)
        }
        controller?.release()
        controller = null
    }

    fun setListener(listener: Player.Listener?) {
        controllerListener?.let { existing ->
            controller?.removeListener(existing)
        }
        controllerListener = listener
        listener?.let { controller?.addListener(it) }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.contentUri)
                .setMediaId(track.id)
                .setTag(track)
                .build()
        }
        controller?.setMediaItems(items, startIndex, 0L)
        controller?.prepare()
        controller?.play()
    }

    fun playPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun toggleShuffle(): Boolean {
        val current = controller?.shuffleModeEnabled ?: false
        val updated = !current
        controller?.shuffleModeEnabled = updated
        return updated
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }
}
