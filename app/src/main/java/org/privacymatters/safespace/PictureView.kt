package org.privacymatters.safespace

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import org.privacymatters.safespace.lib.Constants


class PictureView : AppCompatActivity() {

    private lateinit var imageView: PhotoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        // hide navigation and status bar
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

        imageView = findViewById(R.id.imageView)

        val imagePath = intent.extras?.getString(Constants.INTENT_KEY_PATH)

        Glide.with(applicationContext).load(imagePath).into(imageView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Glide.with(applicationContext).clear(imageView)
                finish()
            }
        })

    }

}