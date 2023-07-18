package org.privacymatters.safespace

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.privacymatters.safespace.lib.Constants

class MediaView : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_view)

        // hide navigation and status bar
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val mediaPath = intent.extras?.getString(Constants.INTENT_KEY_PATH)

        if (mediaPath != null) {
            initializePlayer(mediaPath)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                player?.stop()
                finish()
            }
        })

    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    private fun initializePlayer(path: String) {

        val playerView = findViewById<PlayerView>(R.id.video_view)

        player = ExoPlayer.Builder(this)
            .build()

        // create a media item.
        val mediaItem = MediaItem.fromUri(path)

        player!!.setMediaItem(mediaItem)

        player!!.prepare()

        // Finally assign this media source to the player
        player!!.apply {
            playWhenReady = true // start playing when the exoplayer has setup
            seekTo(0, 0L) // Start from the beginning
            prepare() // Change the state from idle.
        }.also {
            // Do not forget to attach the player to the view
            playerView.player = it
        }
    }
}