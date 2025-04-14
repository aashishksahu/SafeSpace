package org.privacymatters.safespace.media

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
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
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload

class MediaActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val viewModel: MediaActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.activity_media)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        // only open a single item if opened via the pin pin icon
        if (viewModel.ops.lockItem) {
            viewModel.mediaList = listOf(viewModel.mediaList[viewModel.currentPosition])
        }
        // hide navigation and status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Hide the status bar
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            // Allow showing the status bar with swiping from top to bottom
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }


        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.mediaPager)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        viewPager.post {
            viewPager.setCurrentItem(viewModel.currentPosition, false)
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.ops.lockItem) {
                    LockTimer.setLockManually()
                }
                finish()
            }
        })
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = viewModel.mediaList.size
        override fun createFragment(position: Int): Fragment {

            return MediaFragment(
                viewModel.ops.joinPath(
                    viewModel.ops.getInternalPath(),
                    viewModel.mediaList[position].name
                )
            )
        }

    }

    override fun onResume() {
        LockTimer.stop()
        LockTimer.checkLock(this)
        super.onResume()
    }

    override fun onPause() {

        LockTimer.stop()
        LockTimer.start()

        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (listOf(KeyEvent.KEYCODE_ESCAPE, 4).contains(keyCode)) {
            if (viewModel.ops.lockItem) LockTimer.setLockManually()
        }
        return super.onKeyDown(keyCode, event)
    }
}

