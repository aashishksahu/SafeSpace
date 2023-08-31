package org.privacymatters.safespace

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaActionSound
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import org.privacymatters.safespace.lib.Operations
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {

    private lateinit var photoViewFinder: PreviewView
    private lateinit var videoViewFinder: PreviewView

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var ops: Operations
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var audioManager: AudioManager

    private lateinit var imageCaptureButton: ImageButton
    private lateinit var videoCaptureButton: ImageButton
    private lateinit var flashButton: ImageButton

    private lateinit var photoToggleButton: Button
    private lateinit var videoToggleButton: Button

    private lateinit var qualityButton: Button
    private lateinit var fpsButton: Button

    private lateinit var timerText: TextView

    private var preview: Preview? = null

    companion object {
        private const val TAG = "safe_space_"
        private const val IMG_EXTENSION = ".jpg"
        private const val VID_EXTENSION = ".mp4"
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
                startPhotoCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        ops = Operations(application)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        photoViewFinder = findViewById(R.id.photo_view_finder)
        videoViewFinder = findViewById(R.id.video_view_finder)

        imageCaptureButton = findViewById(R.id.image_capture_button)
        videoCaptureButton = findViewById(R.id.video_capture_button)
        flashButton = findViewById(R.id.flash_button)

        photoToggleButton = findViewById(R.id.photo_toggle)
        videoToggleButton = findViewById(R.id.video_toggle)

        qualityButton = findViewById(R.id.quality_selector)
        fpsButton = findViewById(R.id.fps_selector)
        timerText = findViewById(R.id.videoTimer)

        // Select photo mode at startup
        setToPhotoMode()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startPhotoCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for photo and video capture buttons
        imageCaptureButton.setOnClickListener { takePhoto() }

        videoCaptureButton.setOnClickListener { captureVideo(videoCaptureButton) }

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

        photoToggleButton.setOnClickListener {
            setToPhotoMode()
        }

        videoToggleButton.setOnClickListener {
            setToVideoMode()
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
            .format(System.currentTimeMillis()) + IMG_EXTENSION

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

    @SuppressLint("MissingPermission")
    private fun captureVideo(videoCaptureButton: ImageButton) {

        if (recording == null) {

            // Get a stable reference of the modifiable video capture use case
            val videoCapture = videoCapture ?: return
            val timerText = findViewById<TextView>(R.id.videoTimer)

            val recordingListener = Consumer<VideoRecordEvent> { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        videoCaptureButton.setImageResource(R.drawable.record_stop_black_24dp)
                        disableSwitchFromVideoStart()
                        startTimer(true)
                    }

                    is VideoRecordEvent.Finalize -> {
                        startTimer(false)
                        videoCaptureButton.setImageResource(R.drawable.record_start_black_24dp)
                        enableSwitchFromVideoStart()

                        recording?.close()

                        if (event.hasError()) {
                            recording = null
                            Toast.makeText(
                                videoCaptureButton.context,
                                getString(R.string.video_error),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
            }

            // Create time stamped name and MediaStore entry.
            val name = TAG + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis()) + VID_EXTENSION

            val saveLoc =
                File(ops.joinPath(ops.getFilesDir(), ops.getInternalPath(), File.separator, name))

            val fileOutputOptions = FileOutputOptions.Builder(saveLoc).build()

            if (allPermissionsGranted()) {
                recording = videoCapture.output
                    .prepareRecording(videoCaptureButton.context, fileOutputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(this), recordingListener)

            } else {
                requestPermissions()
            }

        }else{
            recording!!.stop()
        }
    }

    private fun startPhotoCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(photoViewFinder.surfaceProvider)
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

    private fun startVideoCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(videoViewFinder.surfaceProvider)
                }

            // video capture
            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.FHD, Quality.HD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
            )

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

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
                    this, cameraSelector, preview, videoCapture
                )

            } catch (_: Exception) {

            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun setToPhotoMode() {
        imageCaptureButton.visibility = Button.VISIBLE
        videoCaptureButton.visibility = Button.GONE
        qualityButton.visibility = Button.GONE
        fpsButton.visibility = Button.GONE
        timerText.visibility = TextView.GONE

        photoToggleButton.setBackgroundColor(
            ContextCompat.getColor(
                photoToggleButton.context,
                R.color.card_background_dark
            )
        )

        videoToggleButton.setBackgroundColor(
            ContextCompat.getColor(
                photoToggleButton.context,
                R.color.translucent
            )
        )

        photoViewFinder.visibility = View.VISIBLE
        videoViewFinder.visibility = View.GONE

        startPhotoCamera()
    }

    private fun setToVideoMode() {
        imageCaptureButton.visibility = Button.GONE
        videoCaptureButton.visibility = Button.VISIBLE
        qualityButton.visibility = Button.VISIBLE
        fpsButton.visibility = Button.VISIBLE
        timerText.visibility = TextView.VISIBLE

        photoToggleButton.setBackgroundColor(
            ContextCompat.getColor(
                photoToggleButton.context,
                R.color.translucent
            )
        )

        videoToggleButton.setBackgroundColor(
            ContextCompat.getColor(
                photoToggleButton.context,
                R.color.card_background_dark
            )
        )

        photoViewFinder.visibility = View.GONE
        videoViewFinder.visibility = View.VISIBLE

        startVideoCamera()
    }

    private fun disableSwitchFromVideoStart() {
        photoToggleButton.isClickable = false
        videoToggleButton.isClickable = false
        fpsButton.isClickable = false
        qualityButton.isClickable = false
    }

    private fun enableSwitchFromVideoStart() {
        photoToggleButton.isClickable = true
        videoToggleButton.isClickable = true
        fpsButton.isClickable = true
        qualityButton.isClickable = true
    }

    private fun startTimer(start: Boolean) {
//        when(start){
//            true -> cameraTimer.start()
//            false -> cameraTimer.reset()
//        }

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

