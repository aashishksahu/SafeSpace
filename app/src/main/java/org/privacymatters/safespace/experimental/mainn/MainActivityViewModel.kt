package org.privacymatters.safespace.experimental.mainn

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.privacymatters.safespace.lib.utils.Constants

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    val ops: DataManager = DataManager.init(application)
    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)
    var longPressAction = false
    val itemList: MutableLiveData<ArrayList<Item>> = MutableLiveData()

    init {

        // initialize at first run of app. Sets the root directory
        if (!sharedPref.getBoolean(Constants.APP_FIRST_RUN, false)) {
            if (ops.initRootDir() == 1) {
                with(sharedPref.edit()) {
                    putBoolean(Constants.APP_FIRST_RUN, true)
                    apply()
                }
            }
        }
        getContents()

    }

    fun getContents() {
        itemList.value = ops.getItems()
    }

    fun travelToLocation(dir: String) {
        val internalPath = ops.internalPath.value
        internalPath?.add(dir)
        ops.internalPath.value = internalPath
    }

    fun returnToPreviousLocation() {
        if (ops.internalPath.value?.last() != Constants.ROOT) {
            val internalPath = ops.internalPath.value
            internalPath?.removeLast()
            ops.internalPath.value = internalPath
        }
    }

}