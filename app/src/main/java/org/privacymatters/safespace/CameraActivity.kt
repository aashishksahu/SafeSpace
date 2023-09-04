package org.privacymatters.safespace

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import org.privacymatters.safespace.cameraUtils.CameraSwitch
import org.privacymatters.safespace.cameraUtils.CameraViewModel
import org.privacymatters.safespace.cameraUtils.PhotoFragment
import org.privacymatters.safespace.cameraUtils.VideoFragment
import org.privacymatters.safespace.lib.Constants


class CameraActivity : AppCompatActivity(), CameraSwitch {

    private lateinit var photoToggleButton: Button
    private lateinit var videoToggleButton: Button
    private var cameraMode = Constants.PHOTO

    private lateinit var photoFragment: PhotoFragment
    private lateinit var videoFragment: VideoFragment
    private lateinit var cameraViewModel: CameraViewModel
    private var videoFragmentFirstCall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraViewModel = CameraViewModel(application)
        photoFragment = PhotoFragment(cameraViewModel)
        videoFragment = VideoFragment(cameraViewModel)

        photoToggleButton = findViewById(R.id.photo_toggle)
        videoToggleButton = findViewById(R.id.video_toggle)


        videoToggleButton.setOnClickListener {
            if (cameraMode == Constants.PHOTO) {
                cameraMode = Constants.VIDEO
                videoToggleButton.setBackgroundColor(
                    ContextCompat.getColor(
                        videoToggleButton.context,
                        R.color.card_background_dark
                    )
                )
                photoToggleButton.setBackgroundColor(
                    ContextCompat.getColor(
                        photoToggleButton.context,
                        R.color.translucent
                    )
                )
                switchMode()
            }
        }

        photoToggleButton.setOnClickListener {
            if (cameraMode == Constants.VIDEO) {
                cameraMode = Constants.PHOTO
                photoToggleButton.setBackgroundColor(
                    ContextCompat.getColor(
                        photoToggleButton.context,
                        R.color.card_background_dark
                    )
                )
                videoToggleButton.setBackgroundColor(
                    ContextCompat.getColor(
                        videoToggleButton.context,
                        R.color.translucent
                    )
                )
                switchMode()
            }
        }

        // default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.fragmentContainer, photoFragment)
            }
        }

    }

    private fun switchMode() {

        val fragment = when (cameraMode) {
            Constants.PHOTO -> photoFragment
            Constants.VIDEO -> videoFragment
            else -> photoFragment
        }

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            if (videoFragmentFirstCall) {
                add(R.id.fragmentContainer, videoFragment)
                videoFragmentFirstCall = false
            } else {
                replace(R.id.fragmentContainer, fragment)
            }
        }


    }

    override fun switchCamera(mode: String) {
        TODO("Not yet implemented")
    }


}

