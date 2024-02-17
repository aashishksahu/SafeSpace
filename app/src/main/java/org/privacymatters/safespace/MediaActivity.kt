package org.privacymatters.safespace

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.privacymatters.safespace.lib.utils.Constants
import org.privacymatters.safespace.lib.mediaManager.MediaPlayer
import org.privacymatters.safespace.lib.fileManager.Operations
import org.privacymatters.safespace.lib.fileManager.Utils
import kotlin.math.abs

class MediaActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var mediaList: ArrayList<String> = ArrayList()
    private lateinit var ops: Operations
    private var currentPosition = 0
    private var previousPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        // hide navigation and status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Hide the status bar
            hide(WindowInsetsCompat.Type.statusBars())
            // Allow showing the status bar with swiping from top to bottom
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        ops = Operations(application)

        var (mediaListTemp, _) = ops.getContents(ops.getInternalPath())

        val intentMediaPath = intent.extras?.getString(Constants.INTENT_KEY_PATH)

        mediaListTemp = mediaListTemp.filter { item ->
            Utils.getFileType(item.name) in listOf(
                Constants.IMAGE_TYPE,
                Constants.VIDEO_TYPE,
                Constants.AUDIO_TYPE
            )
        }


        mediaListTemp.forEachIndexed { index, mediaItem ->

            val tempMediaItem = ops.joinPath(
                ops.getFilesDir(),
                ops.getInternalPath(),
                mediaItem.name
            )

            if (tempMediaItem == intentMediaPath) {
                currentPosition = index
            }

            mediaList.add(tempMediaItem)
        }

        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.mediaPager)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        viewPager.post {
            viewPager.setCurrentItem(currentPosition, true)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                if(abs(position-previousPosition) == 1){
                    MediaPlayer.player?.release()
                }

            }

            override fun onPageSelected(position: Int) {
                previousPosition = position
            }
        })

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            })
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = mediaList.size

        override fun createFragment(position: Int): Fragment = MediaFragment(mediaList[position])

    }


}

