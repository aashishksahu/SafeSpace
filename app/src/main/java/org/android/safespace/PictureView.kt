package org.android.safespace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import org.android.safespace.lib.Constants


class PictureView : AppCompatActivity() {

    private lateinit var imageView: PhotoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        imageView = findViewById(R.id.imageView)

        val imagePath = intent.extras?.getString(Constants.INTENT_KEY_PATH)

        Glide.with(applicationContext).load(imagePath).into(imageView)

    }

    override fun onStop() {
        super.onStop()

        Glide.with(applicationContext).clear(imageView)
    }
}