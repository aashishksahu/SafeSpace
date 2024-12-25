package org.privacymatters.safespace.camera

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaActionSound
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import org.privacymatters.safespace.R
import org.privacymatters.safespace.main.DataManager
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload
import org.privacymatters.safespace.utils.Utils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {

    private var recorder: Recorder? = null
    private var videoFlashOn: Boolean = false
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val ops = DataManager

    private lateinit var photoToggleButton: Button
    private lateinit var videoToggleButton: Button
    private var cameraMode: String? = null

    private lateinit var cameraViewModel: CameraViewModel

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var audioManager: AudioManager
    private lateinit var shutterButton: ImageButton
    private lateinit var switchCameraButton: ImageButton

    private lateinit var flashButton: ImageButton

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var preview: Preview? = null

    private lateinit var qualityButton: Button
    private lateinit var timerText: TextView

    companion object {
        private const val TAG = "safe_space_"
        private const val IMG_EXTENSION = ".jpg"
        private const val VID_EXTENSION = ".mp4"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
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
                    applicationContext,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.activity_camera)

        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.black)
        window.navigationBarColor = ContextCompat.getColor(applicationContext, R.color.black)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        ops.ready(application)

        val cameraSelectorText = intent.getStringExtra(Constants.CAMERA_SELECTOR)
        cameraMode = intent.getStringExtra(Constants.CAMERA_MODE)

        cameraViewModel = CameraViewModel(application, getString(R.string.video_timer))

        photoToggleButton = findViewById(R.id.photo_toggle)
        videoToggleButton = findViewById(R.id.video_toggle)

        audioManager =
            applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        viewFinder = findViewById(R.id.view_finder)

        flashButton = findViewById(R.id.flash_button)
        shutterButton = findViewById(R.id.shutter_button)
        switchCameraButton = findViewById(R.id.switch_camera)

        qualityButton = findViewById(R.id.quality_selector)
        timerText = findViewById(R.id.videoTimer)

        cameraSelector = if (cameraSelectorText == Constants.DEFAULT_BACK_CAMERA ||
            cameraSelectorText.isNullOrEmpty()
        ) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        if (cameraMode.isNullOrEmpty()) {
            cameraMode = Constants.PHOTO
        }
        switchMode()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for photo and video capture buttons
        shutterButton.setOnClickListener {

            when (cameraMode) {
                Constants.PHOTO -> takePhoto()
                Constants.VIDEO -> captureVideo(shutterButton)
            }

        }

        switchCameraButton.setOnClickListener {

            val cameraSelectorName = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                Constants.DEFAULT_FRONT_CAMERA
            } else {
                Constants.DEFAULT_BACK_CAMERA
            }

            intent.putExtra(Constants.CAMERA_SELECTOR, cameraSelectorName)
            intent.putExtra(Constants.CAMERA_MODE, cameraMode)
            recreate()
        }

        flashButton.setOnClickListener {

            if (imageCapture != null && cameraMode == Constants.PHOTO) {
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
            } else if (cameraMode == Constants.VIDEO) {
                val videoCaptureRef = videoCapture
                when (videoFlashOn) {
                    false -> {
                        videoCaptureRef?.camera?.cameraControl?.enableTorch(true)
                        videoFlashOn = true
                        flashButton.setImageResource(R.drawable.flash_on_black_24dp)
                    }

                    true -> {
                        videoCaptureRef?.camera?.cameraControl?.enableTorch(false)
                        videoFlashOn = false
                        flashButton.setImageResource(R.drawable.flash_off_black_24dp)
                    }

                }
            }
        }

        videoToggleButton.setOnClickListener {
            if (cameraMode == Constants.PHOTO) {
                cameraMode = Constants.VIDEO
                switchMode()
            }
        }

        photoToggleButton.setOnClickListener {
            if (cameraMode == Constants.VIDEO) {
                cameraMode = Constants.PHOTO
                switchMode()
            }
        }

        qualityButton.setOnClickListener {
            val videoCaptureRef = videoCapture
            val qualitySelector: QualitySelector

            if (videoCaptureRef != null) {
                when (qualityButton.text) {
                    getString(R.string.q1080p) -> {
                        qualityButton.text = getString(R.string.q480p)
                        qualitySelector = QualitySelector.from(
                            Quality.SD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                        )
                        recorder = Recorder.Builder()
                            .setQualitySelector(qualitySelector)
                            .build()
                    }

                    getString(R.string.q480p) -> {
                        qualityButton.text = getString(R.string.q720p)
                        qualitySelector = QualitySelector.from(
                            Quality.HD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                        )
                        recorder = Recorder.Builder()
                            .setQualitySelector(qualitySelector)
                            .build()
                    }

                    getString(R.string.q720p) -> {
                        qualityButton.text = getString(R.string.q1080p)
                        qualitySelector = QualitySelector.from(
                            Quality.FHD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
                        )
                        recorder = Recorder.Builder()
                            .setQualitySelector(qualitySelector)
                            .build()
                    }
                }
                startCamera()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun switchMode() {

        if (cameraMode == Constants.PHOTO) {
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

            qualityButton.visibility = View.GONE
            timerText.visibility = View.GONE
            shutterButton.setImageResource(R.drawable.capture_64)

        } else if (cameraMode == Constants.VIDEO) {
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

            qualityButton.visibility = View.VISIBLE
            timerText.visibility = View.VISIBLE
            shutterButton.setImageResource(R.drawable.record_start)

        }

        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            flashButton.visibility = View.GONE
        } else {
            flashButton.visibility = View.VISIBLE
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // video capture
            val qualitySelector = QualitySelector.from(
                Quality.SD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )

            if (recorder == null) {
                recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
            }

            videoCapture = VideoCapture.withOutput(recorder!!)

            cameraViewModel.timerCounterText.observe(this) { timeCount ->
                timerText.text = timeCount
            }

            // image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

            } catch (e: Exception) {
                Utils.exportToLog(application, "@CameraActivity.startCamera()", e)
            }

        }, ContextCompat.getMainExecutor(applicationContext))
    }

    private fun takePhoto() {

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name =
            TAG + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis()) + IMG_EXTENSION

        val saveLoc =
            File(ops.joinPath(ops.getInternalPath(), name))

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLoc).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Utils.exportToLog(application, "@CameraActivity.takePhoto()", exc)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

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
    private fun captureVideo(shutterButton: ImageButton) {

        if (recording == null) {

            // Get a stable reference of the modifiable video capture use case
            val videoCapture = videoCapture ?: return
            timerText = findViewById(R.id.videoTimer)

            val recordingListener = Consumer<VideoRecordEvent> { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        shutterButton.setImageResource(R.drawable.record_stop)
                        disableSwitchFromVideoStart()
                        when (audioManager.ringerMode) {
                            AudioManager.RINGER_MODE_NORMAL -> {
                                val sound = MediaActionSound()
                                sound.play(MediaActionSound.START_VIDEO_RECORDING)
                            }
                        }
                        cameraViewModel.startTimer()
                    }

                    is VideoRecordEvent.Finalize -> {
                        shutterButton.setImageResource(R.drawable.record_start)
                        enableSwitchFromVideoStart()

                        when (audioManager.ringerMode) {
                            AudioManager.RINGER_MODE_NORMAL -> {
                                val sound = MediaActionSound()
                                sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                            }
                        }

                        if (event.hasError()) {
                            Utils.exportToLog(
                                application,
                                "@CameraActivity.captureVideo()\n${event.error} - ${event.cause}",
                                null
                            )
                            Toast.makeText(
                                shutterButton.context,
                                getString(R.string.video_error),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }

                        recording?.close()
                        recording = null
                        cameraViewModel.stopTimer()
                    }
                }
            }

            // Create time stamped name and MediaStore entry.
            val name = TAG + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis()) + VID_EXTENSION

            val saveLoc =
                File(ops.joinPath(ops.getInternalPath(), name))

            val fileOutputOptions = FileOutputOptions.Builder(saveLoc).build()

            if (allPermissionsGranted()) {
                recording = videoCapture.output
                    .prepareRecording(shutterButton.context, fileOutputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(applicationContext), recordingListener)

            } else {
                requestPermissions()
            }

        } else {
            recording!!.stop()
        }
    }

    private fun disableSwitchFromVideoStart() {
        qualityButton.isClickable = false
        flashButton.isClickable = false
        switchCameraButton.isClickable = false
    }

    private fun enableSwitchFromVideoStart() {
        qualityButton.isClickable = true
        flashButton.isClickable = true
        switchCameraButton.isClickable = true
    }


    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            applicationContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
}

