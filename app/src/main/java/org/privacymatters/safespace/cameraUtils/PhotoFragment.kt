package org.privacymatters.safespace.cameraUtils

import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ExifInterface
import android.media.MediaActionSound
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.Constants
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil


class PhotoFragment(private val viewModel: CameraViewModel) : Fragment() {

    private lateinit var photoViewFinder: PreviewView
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var audioManager: AudioManager

    private lateinit var shutterButton: ImageButton
    private lateinit var flashButton: ImageButton

    private lateinit var photoToggleButton: Button
    private lateinit var videoToggleButton: Button

    private var preview: Preview? = null

    private var rotationDegrees = 0

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
        val view = inflater.inflate(R.layout.fragment_photo, container, false)

        audioManager =
            requireContext().getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
        photoViewFinder = view.findViewById(R.id.photo_view_finder)

        shutterButton = view.findViewById(R.id.shutter_button)
        flashButton = view.findViewById(R.id.flash_button)

        photoToggleButton = view.findViewById(R.id.photo_toggle)
        videoToggleButton = view.findViewById(R.id.video_toggle)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } else {
            requestPermissions()
        }

        // Set up the listeners for photo and video capture buttons
        shutterButton.setOnClickListener { takePhoto() }

        videoToggleButton.setOnClickListener {
            viewModel.setCameraMode(Constants.VIDEO)
        }

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

            // image capture
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(viewPortWidthScaled, viewPortHeightScaled))
                .build()

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

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = TAG + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis()) + EXTENSION

        val saveLoc = viewModel.getPath(name)

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLoc).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    imageProxy.close()
                }
            })

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {}

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

//                    val savedImage = File(outputFileResults.savedUri.toString())
                    val exifData = ExifInterface(outputFileResults.savedUri?.path!!)
                    when (rotationDegrees) {
                        in 91..180 -> {
                            exifData.setAttribute(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_ROTATE_90.toString()
                            )
                        }
                        in 181..270 -> {
                            exifData.setAttribute(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_ROTATE_180.toString()
                            )
                        }
                        in 271..360 -> {
                            exifData.setAttribute(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_ROTATE_270.toString()
                            )
                        }
                    }
                    exifData.saveAttributes()

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