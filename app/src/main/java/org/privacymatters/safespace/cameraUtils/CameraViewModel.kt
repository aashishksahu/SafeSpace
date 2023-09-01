package org.privacymatters.safespace.cameraUtils

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.AndroidViewModel
import org.privacymatters.safespace.lib.Constants
import org.privacymatters.safespace.lib.Operations
import java.io.File
import kotlin.math.ceil

class CameraViewModel(application: Application, private val cameraSwitch: CameraSwitch) : AndroidViewModel(application) {

    private var cameraMode = Constants.PHOTO // Default mode
    private var ops: Operations

    init {
        ops = Operations(application)
    }

    fun getCameraMode(): String = cameraMode

    fun setCameraMode(mode: String) {
        cameraMode = mode

        cameraSwitch.switchMode(mode)

    }

    fun getPath(name: String): File{
        return File(ops.joinPath(ops.getFilesDir(), ops.getInternalPath(), File.separator, name))
    }

    fun getScreenWidth(): Int = Resources.getSystem().displayMetrics.widthPixels

    fun getScreenHeight(): Int {
        val height = Resources.getSystem().displayMetrics.heightPixels

        return ceil(height - (height * 0.07)).toInt()

    }

}