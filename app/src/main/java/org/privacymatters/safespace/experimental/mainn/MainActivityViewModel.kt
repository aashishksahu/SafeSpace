package org.privacymatters.safespace.experimental.mainn

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import org.privacymatters.safespace.lib.utils.Constants

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var ops = DataManager
    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)
    var longPressAction = false

    private val _itemList: SnapshotStateList<Item> = mutableStateListOf()
    val itemList: List<Item> = _itemList

    init {
        ops.ready(application)
        getItems()
    }

    fun getItems() {
        _itemList.clear()
        _itemList.addAll(ops.getItems())
    }

    fun travelToLocation(dir: String) {
        ops.internalPath.add(dir)
    }

    fun returnToPreviousLocation() {
        if (ops.internalPath.last() != Constants.ROOT) {
            ops.internalPath.removeLast()
        }
    }

    fun moveToDestination() {

    }

    fun copyToDestination() {

    }

    fun shareFiles() {

    }

    fun exportSelection() {

    }

    fun clearSelection() {

    }

    fun copyItems() {

    }

    fun moveItems() {

    }

    fun deleteItems() {

    }

    fun createTextNote() {

    }

    fun createFolder() {

    }

    fun importFiles() {

    }

}