package org.android.safespace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import org.android.safespace.lib.Constants


class ImageView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        val imageView = findViewById<PhotoView>(R.id.imageView)

        val imagePath = intent.extras?.getString(Constants.INTENT_KEY_PATH)

        Glide.with(this).load(imagePath).into(imageView)

    }
}