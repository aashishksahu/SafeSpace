package org.android.safespace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import org.android.safespace.lib.Constants
import org.android.safespace.lib.Utils

class MediaView : AppCompatActivity() {

    private var player: ExoPlayer? = null
//    private val isPlaying get() = player?.isPlaying ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_view)

        val mediaPath = intent.extras?.getString(Constants.INTENT_KEY_PATH)

        if (mediaPath != null) {
            initializePlayer(mediaPath)
        }

    }

    private fun initializePlayer(path: String) {

        val playerView = findViewById<StyledPlayerView>(R.id.video_view)

        player = ExoPlayer.Builder(this)
            .build()

        // create a media item.
        val mediaItem = MediaItem.Builder()
            .setUri(path)
            .setMimeType(Utils.getMimeType(path))
            .build()

        // Create a media source and pass the media item
        val mediaSource = ProgressiveMediaSource.Factory(
            DefaultDataSource.Factory(this)
        ).createMediaSource(mediaItem)

        // Finally assign this media source to the player
        player!!.apply {
            setMediaSource(mediaSource)
            playWhenReady = true // start playing when the exoplayer has setup
            seekTo(0, 0L) // Start from the beginning
            prepare() // Change the state from idle.
        }.also {
            // Do not forget to attach the player to the view
            playerView.player = it
        }
    }
}