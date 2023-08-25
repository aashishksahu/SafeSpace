package org.privacymatters.safespace

import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaActionSound
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import org.privacymatters.safespace.lib.Operations
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var ops: Operations
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var audioManager: AudioManager

    companion object {
        private const val TAG = "safe_space_"
        private const val EXTENSION = ".jpg"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ).toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        ops = Operations(application)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val imageCaptureButton = findViewById<ImageButton>(R.id.image_capture_button)
        val videoCaptureButton = findViewById<ImageButton>(R.id.video_capture_button)
        val flashButton = findViewById<ImageButton>(R.id.flash_button)

        val cameraModeToggleButton = findViewById<MaterialButtonToggleGroup>(R.id.toggleButton)
        cameraModeToggleButton.check(R.id.photo_toggle)

        val photoButton = findViewById<Button>(R.id.photo_toggle)
        val videoButton = findViewById<Button>(R.id.video_toggle)

        val qualityButton = findViewById<Button>(R.id.quality_selector)
        val fpsButton = findViewById<Button>(R.id.fps_selector)

        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.FHD, Quality.HD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD))

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }


        // Set up the listeners for take photo and video capture buttons
        imageCaptureButton.setOnClickListener { takePhoto() }

        videoCaptureButton.setOnClickListener { captureVideo() }

        flashButton.setOnClickListener {
            if (imageCapture != null) {
                when (imageCapture!!.flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> {
                        imageCapture!!.flashMode = ImageCapture.FLASH_MODE_ON
                        flashButton.setImageResource(R.drawable.flash_on_black_24dp)
                    }

                    ImageCapture.FLASH_MODE_ON -> {
                        imageCapture!!.flashMode = ImageCapture.FLASH_MODE_AUTO
                        flashButton.setImageResource(R.drawable.flash_auto_black_24dp)
                    }

                    ImageCapture.FLASH_MODE_AUTO -> {
                        imageCapture!!.flashMode = ImageCapture.FLASH_MODE_OFF
                        flashButton.setImageResource(R.drawable.flash_off_black_24dp)
                    }

                    else -> ImageCapture.FLASH_MODE_AUTO
                }
            }
        }

        cameraModeToggleButton.addOnButtonCheckedListener { _, checkedId, _ ->
            when (checkedId) {
                R.id.photo_toggle -> setToPhotoMode(
                    photoButton,
                    videoButton,
                    qualityButton,
                    fpsButton
                )

                R.id.video_toggle -> setToVideoMode(
                    photoButton,
                    videoButton,
                    qualityButton,
                    fpsButton
                )
            }
        }

        // Todo: Add options to choose quality and fps
        qualityButton.setOnClickListener {
            val imageCaptureRef = imageCapture
            if (imageCaptureRef != null) {
                when (qualityButton.text) {
                    getString(R.string.q1080p) -> {
                        qualityButton.text = getString(R.string.q720p)
                    }
                    getString(R.string.q720p) -> {
                        qualityButton.text = getString(R.string.q1080p)
                    }
                }
            }
        }

        fpsButton.setOnClickListener {
            val imageCaptureRef = imageCapture
            if (imageCaptureRef != null) {
                when (fpsButton.text) {
                    getString(R.string.fps30) -> {
                        fpsButton.text = getString(R.string.fps60)
                    }
                    getString(R.string.fps60) -> {
                        fpsButton.text = getString(R.string.fps30)
                    }
                }
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun takePhoto() {

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = TAG + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis()) + EXTENSION

        val saveLoc =
            File(ops.joinPath(ops.getFilesDir(), ops.getInternalPath(), File.separator, name))

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLoc).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {}

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    when (audioManager.ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> {
                            val sound = MediaActionSound()
                            sound.play(MediaActionSound.SHUTTER_CLICK)
                        }
                    }
                }
            }
        )
    }

    private fun captureVideo() {}

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val viewFinder = findViewById<PreviewView>(R.id.viewFinder)
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // image capture
            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (_: Exception) {

            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun setToPhotoMode(
        photoButton: Button,
        videoButton: Button,
        qualityButton: Button,
        fpsButton: Button
    ) {
        photoButton.visibility = Button.VISIBLE
        videoButton.visibility = Button.GONE
        qualityButton.visibility = Button.GONE
        fpsButton.visibility = Button.GONE
    }

    private fun setToVideoMode(
        photoButton: Button,
        videoButton: Button,
        qualityButton: Button,
        fpsButton: Button
    ) {
        photoButton.visibility = Button.GONE
        videoButton.visibility = Button.VISIBLE
        qualityButton.visibility = Button.VISIBLE
        fpsButton.visibility = Button.VISIBLE
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}