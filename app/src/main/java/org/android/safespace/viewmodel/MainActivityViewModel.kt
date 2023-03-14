package org.android.safespace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var internalPath: String = ""

    fun getInternalPath(): String {
        return internalPath
    }

    fun setInternalPath(dir: String) {
        internalPath = dir
    }

}