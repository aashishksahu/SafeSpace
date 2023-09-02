package org.privacymatters.safespace.cameraUtils

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
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
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.Constants
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil

class VideoFragment(private val viewModel: CameraViewModel) : Fragment() {

    private lateinit var photoViewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var audioManager: AudioManager

    private lateinit var shutterButton: ImageButton
    private lateinit var flashButton: ImageButton

    private lateinit var photoToggleButton: Button
    private lateinit var videoToggleButton: Button

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var preview: Preview? = null

    private lateinit var qualityButton: Button
    private lateinit var fpsButton: Button
    private lateinit var timerText: TextView

    companion object {
        private const val TAG = "safe_space_"
        private const val EXTENSION = ".mp4"
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
                    context,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            }
        }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_video, container, false)

        audioManager =
            requireContext().getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
        photoViewFinder = view.findViewById(R.id.video_view_finder)

        shutterButton = view.findViewById(R.id.shutter_button_v)
        flashButton = view.findViewById(R.id.flash_button_v)

        photoToggleButton = view.findViewById(R.id.photo_toggle_v)
        videoToggleButton = view.findViewById(R.id.video_toggle_v)

        qualityButton = view.findViewById(R.id.quality_selector)
        fpsButton = view.findViewById(R.id.fps_selector)
        timerText = view.findViewById(R.id.videoTimer)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } else {
            requestPermissions()
        }

        // Set up the listeners for photo and video capture buttons
        shutterButton.setOnClickListener { captureVideo(shutterButton) }

        photoToggleButton.setOnClickListener {
            viewModel.setCameraMode(Constants.PHOTO)
        }

        // Todo: Add options to choose quality and fps
        qualityButton.setOnClickListener {
            val videoCaptureRef = videoCapture
            if (videoCaptureRef != null) {
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
            val videoCaptureRef = videoCapture
            if (videoCaptureRef != null) {
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

        flashButton.setOnClickListener {

            val cameraController = LifecycleCameraController(requireContext())

            if (imageCapture != null) {
                when (imageCapture!!.flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> {
                        cameraController.enableTorch(true)
                        flashButton.setImageResource(R.drawable.flash_on_black_24dp)
                    }

                    ImageCapture.FLASH_MODE_ON -> {
                        cameraController.enableTorch(false)
                        flashButton.setImageResource(R.drawable.flash_off_black_24dp)
                    }

                    else -> cameraController.enableTorch(false)
                }
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        return view
    }

    private fun startCamera(
        cameraSelector: CameraSelector
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val viewPortHeightScaled = ceil(photoViewFinder.height * 1.2).toInt()
            val viewPortWidthScaled = ceil(photoViewFinder.width * 1.2).toInt()

            // Preview
            preview = Preview.Builder()
                .setTargetResolution(Size(viewPortWidthScaled, viewPortHeightScaled))
                .build()
                .also {
                    it.setSurfaceProvider(photoViewFinder.surfaceProvider)
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

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("MissingPermission")
    private fun captureVideo(shutterButton: ImageButton) {

        if (recording == null) {

            // Get a stable reference of the modifiable video capture use case
            val videoCapture = videoCapture ?: return
            val timerText = view?.findViewById<TextView>(R.id.videoTimer)

            val recordingListener = Consumer<VideoRecordEvent> { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        shutterButton.setImageResource(R.drawable.record_stop_black_24dp)
                        disableSwitchFromVideoStart()
                        startTimer(true)
                    }

                    is VideoRecordEvent.Finalize -> {
                        startTimer(false)
                        shutterButton.setImageResource(R.drawable.record_start_black_24dp)
                        enableSwitchFromVideoStart()

                        recording?.close()

                        if (event.hasError()) {
                            recording = null
                            Toast.makeText(
                                shutterButton.context,
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
                .format(System.currentTimeMillis()) + EXTENSION

            val saveLoc = viewModel.getPath(name)

            val fileOutputOptions = FileOutputOptions.Builder(saveLoc).build()

            if (allPermissionsGranted()) {
                recording = videoCapture.output
                    .prepareRecording(shutterButton.context, fileOutputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(requireContext()), recordingListener)

            } else {
                requestPermissions()
            }

        } else {
            recording!!.stop()
        }
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
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}