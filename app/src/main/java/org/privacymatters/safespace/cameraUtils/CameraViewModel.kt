package org.privacymatters.safespace.cameraUtils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.privacymatters.safespace.lib.Operations
import java.io.File

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private var ops: Operations

    init {
        ops = Operations(application)
    }

    fun getPath(name: String): File {
        return File(ops.joinPath(ops.getFilesDir(), ops.getInternalPath(), File.separator, name))
    }

}