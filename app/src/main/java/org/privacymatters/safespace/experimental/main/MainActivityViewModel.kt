package org.privacymatters.safespace.experimental.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.privacymatters.safespace.utils.Constants
import java.io.File

enum class ActionBarType {
    NORMAL, LONG_PRESS, MOVE, COPY
}

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var fileSortBy = Constants.NAME
    private var fileSortOrder = Constants.ASC
    private var ops = DataManager
    var itemList: List<Item> = ops.baseItemList

    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

    // 0: NormalActionBar, 1: LongPressActionBar, 2: MoveActionBar, 3: CopyActionBar
    var appBarType = mutableStateOf(ActionBarType.NORMAL)
    var scrollToPosition = ops.positionHistory


    private val _internalPathList: SnapshotStateList<String> = mutableStateListOf()
    val internalPathList: List<String> = _internalPathList

    init {
        ops.ready(application)
        getItems()
        getInternalPath()
    }

    fun getItems() {
        sortItems(fileSortBy, fileSortOrder)
    }

    fun sortItems(sortBy: String, sortOrder: String) {
        ops.getSortedItems(fileSortBy, fileSortOrder)
        ops.itemStateList.clear()
        ops.itemStateList.addAll(ops.baseItemList)

        sharedPref.edit()
            .putString(Constants.FILE_SORT_BY, sortBy)
            .putString(Constants.FILE_SORT_ORDER, sortOrder)
            .apply()
    }

    fun getInternalPath() {
        _internalPathList.clear()
        _internalPathList.addAll(ops.getInternalPathList())
    }

    fun getIconPath(fileName: String): String {
        return ops.joinPath(ops.getInternalPath(), fileName)
    }

    fun travelToLocation(dir: String) {
        ops.internalPath.add(dir)
        getItems()
        getInternalPath()
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

    fun copyItems() {

    }

    fun moveItems() {

    }

    fun deleteItems() {

    }

    fun createTextNote(name: String): File {
        val fileName = name + "." + Constants.TXT

        val noteFile = File(ops.joinPath(ops.getInternalPath(), fileName))

        if (!noteFile.exists()) {
            noteFile.createNewFile()
        }
        return noteFile
    }

    fun createFolder(name: String): File {

        val folder = File(ops.joinPath(ops.getInternalPath(), name))

        if (!folder.exists()) {
            folder.mkdirs()
            getItems()
        } else {
            throw Exception(Constants.ALREADY_EXISTS)
        }
        return folder
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
                    ops.importFile(importList[i])
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

    fun setSelected(item: Item) {

        ops.baseItemList.find { it == item }?.isSelected = true

        ops.itemStateList.clear()
        ops.itemStateList.addAll(ops.baseItemList)
    }

    fun setUnSelected(item: Item) {

        ops.baseItemList.find { it == item }?.isSelected = false
        ops.itemStateList.clear()
        ops.itemStateList.addAll(ops.baseItemList)
        if (ops.baseItemList.all { !it.isSelected }) {
            appBarType.value = ActionBarType.NORMAL
        }
    }

    fun clearSelection() {

        ops.baseItemList.forEach { it.isSelected = false }
        ops.itemStateList.clear()
        ops.itemStateList.addAll(ops.baseItemList)
    }
}