package org.privacymatters.safespace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import org.privacymatters.safespace.cameraUtils.CameraSwitch
import org.privacymatters.safespace.cameraUtils.CameraViewModel
import org.privacymatters.safespace.cameraUtils.PhotoFragment
import org.privacymatters.safespace.cameraUtils.VideoFragment
import org.privacymatters.safespace.lib.Constants


class CameraActivity : AppCompatActivity(), CameraSwitch {



    private lateinit var photoFragment: PhotoFragment
    private lateinit var videoFragment: VideoFragment
    private lateinit var cameraViewModel: CameraViewModel
    private var videoFragmentFirstCall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraViewModel = CameraViewModel(application, this)
        photoFragment = PhotoFragment(cameraViewModel)
        videoFragment = VideoFragment(cameraViewModel)


        // default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.fragmentContainer, photoFragment)
            }
        }

    }

    override fun switchMode(mode: String){

        when(mode){
            Constants.PHOTO -> {
                supportFragmentManager.commit {
                    // no subsFragmentFirstCall check for expenses fragment as
                    // it is already set as default on activity start
                    setReorderingAllowed(true)
                    attach(photoFragment)
                }
            }
            Constants.VIDEO -> {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    if (videoFragmentFirstCall) {
                        add(R.id.fragmentContainer, videoFragment)
                        videoFragmentFirstCall = false
                    } else {
                        attach(videoFragment)
                    }
                }
            }
        }


    }

    override fun switchCamera(mode: String) {
        TODO("Not yet implemented")
    }


}

