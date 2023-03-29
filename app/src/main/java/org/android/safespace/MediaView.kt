package org.android.safespace

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.exoplayer2.ExoPlayer

class MediaView : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private val isPlaying get() = player?.isPlaying ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_view)

//        https://medium.com/codex/exoplayer-in-android-2022-getting-started-6edcb2b399e5

    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
    }
}