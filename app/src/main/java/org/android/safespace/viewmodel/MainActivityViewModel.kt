package org.android.safespace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import java.io.File

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var internalPath: ArrayList<String> = ArrayList()

    fun getInternalPath(): String {
        return internalPath.joinToString(File.separator)
    }

    fun setInternalPath(dir: String) {
        if (internalPath.isEmpty()) {
            internalPath.add(dir)
        } else if (internalPath.last() != dir) {
            internalPath.add(dir)
        }
    }

    fun setPreviousPath() {
        if (internalPath.isNotEmpty()) internalPath.removeLast()
    }

}