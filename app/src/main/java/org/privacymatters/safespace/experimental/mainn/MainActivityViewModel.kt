package org.privacymatters.safespace.experimental.mainn

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.privacymatters.safespace.lib.utils.Constants

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var ops = DataManager

    //    private val sharedPref: SharedPreferences =
//        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)
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
        getItems()
    }

    fun returnToPreviousLocation() {
        if (ops.internalPath.last() != Constants.ROOT) {
            ops.internalPath.removeLast()
        }
    }

    fun getPath(name: String): String {
        return ops.joinPath(ops.getInternalPath(), name)
    }

    fun extractZip(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            when (ops.extractZip(path)) {
                true -> getItems()
                false -> getItems()
            }
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

    fun importFiles(result: ActivityResult) {

        val importList: ArrayList<Uri> = ArrayList()

        if (result.resultCode == AppCompatActivity.RESULT_OK) {

            val data: Intent? = result.data

            //If multiple files selected
            if (data?.clipData != null) {
                val count = data.clipData?.itemCount ?: 0

                for (i in 0 until count) {
                    importList.add(data.clipData?.getItemAt(i)?.uri!!)
                }
            } else if (data?.data != null) {
                //If single file selected
                importList.add(data.data!!)
            }

            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0 until importList.size) {
                    ops.importFile(importList[i], ops.getInternalPath())
                }
                CoroutineScope(Dispatchers.Main).launch {
                    getItems()
                }
            }
        }
    }

    fun isRootDirectory(): Boolean {
        return ops.internalPath.size == 1 && ops.internalPath[0] == Constants.ROOT
    }

}