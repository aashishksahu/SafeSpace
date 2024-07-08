package org.privacymatters.safespace.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import org.privacymatters.safespace.R
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.Utils

class MediaFragment : Fragment() {

    private lateinit var photoView: PhotoView
    private lateinit var playerView: PlayerView
    private val viewModel: MediaActivityViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Glide.with(requireContext()).clear(photoView)
                MediaPlayer.player?.release()
                MediaPlayer.player = null
                activity?.finish()
            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_media, container, false)

        photoView = view.findViewById(R.id.imageView)
        playerView = view.findViewById(R.id.video_view)

        playMedia(viewModel.getMediaPath())

        return view
    }

    private fun playMedia(path: String) {
        when (Utils.getFileType(path)) {
            Constants.IMAGE_TYPE -> setToPhotoView(path, photoView, playerView)

            Constants.VIDEO_TYPE,
            Constants.AUDIO_TYPE -> setToPlayerView(path, photoView, playerView)
        }
    }

    private fun setToPhotoView(path: String, photoView: PhotoView, playerView: PlayerView) {
        photoView.visibility = View.VISIBLE
        playerView.visibility = View.GONE

        Glide.with(requireContext()).load(path).into(photoView)
    }

    private fun setToPlayerView(path: String, photoView: PhotoView, playerView: PlayerView) {
        photoView.visibility = View.GONE
        playerView.visibility = View.VISIBLE

        initializePlayer(path)
    }

    override fun onResume() {
        super.onResume()
        playMedia(viewModel.getMediaPath())
    }

    override fun onStop() {
        super.onStop()
        Glide.with(requireContext()).clear(photoView)
    }

    override fun onDestroy() {
        MediaPlayer.player?.release()
        MediaPlayer.player = null
        super.onDestroy()
    }

    private fun initializePlayer(path: String) {

        MediaPlayer.player?.release()

        MediaPlayer.player = ExoPlayer.Builder(requireContext())
            .build()

        // create a media item.
        val mediaItem = MediaItem.fromUri(path)

        MediaPlayer.player!!.setMediaItem(mediaItem)

        MediaPlayer.player!!.prepare()

        // Finally assign this media source to the player
        MediaPlayer.player!!.apply {
            playWhenReady = true // start playing when the exoplayer has setup
            seekTo(0, 0L) // Start from the beginning
            prepare() // Change the state from idle.
        }.also {
            // Do not forget to attach the player to the view
            playerView.player = it
        }
    }
}
