package org.privacymatters.safespace.media

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.privacymatters.safespace.R

class MediaActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val viewModel: MediaActivityViewModel by viewModels()

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

//        val firstPosition = intent.extras?.getInt(Constants.INTENT_KEY_INDEX) ?: 0

        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.mediaPager)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val position = viewModel.currentPosition

        viewPager.post {
            viewPager.setCurrentItem(position, false)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                // release player if scrolled out (except for when it is the first of the last item)
                if (position > 0 && position < viewModel.mediaList.lastIndex) {
                    MediaPlayer.player?.release()
                }
                viewModel.setPosition(position)
            }
        })
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = viewModel.mediaList.size
        override fun createFragment(position: Int): Fragment =
            MediaFragment(
                viewModel.ops.joinPath(
                    viewModel.ops.getInternalPath(),
                    viewModel.mediaList[position].name
                )
            )

    }
}

