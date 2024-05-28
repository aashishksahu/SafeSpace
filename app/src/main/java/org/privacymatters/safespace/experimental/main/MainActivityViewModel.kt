package org.privacymatters.safespace.experimental.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.FileUtils
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.utils.Constants
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

enum class ActionBarType {
    NORMAL, LONG_PRESS, MOVE, COPY
}

class MainActivityViewModel(private val application: Application) : AndroidViewModel(application) {

    private var fileSortBy = Constants.NAME
    private var fileSortOrder = Constants.ASC
    var ops = DataManager
    var itemList: List<Item> = ops.baseItemList
    var transferList: ArrayList<Item> = arrayListOf()

    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

    // 0: NormalActionBar, 1: LongPressActionBar, 2: MoveActionBar, 3: CopyActionBar
    var appBarType = mutableStateOf(ActionBarType.NORMAL)
    var scrollToPosition = ops.positionHistory
    var selectedFileCount = mutableIntStateOf(0)
    var selectedFolderCount = mutableIntStateOf(0)

    private var fromPath = ""

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

    fun moveToDestination(): FileOpCode {
        var status = FileOpCode.SUCCESS

        val toPath = ops.getInternalPath()

        if (fromPath == toPath) {
            transferList.clear()
            return FileOpCode.SAME_PATH
        }

        try {
            for (item in transferList) {
                if (item.isSelected) {

                    if (File(ops.joinPath(toPath, item.name)).exists()) {
                        status = FileOpCode.EXISTS
                    } else {
                        Files.move(
                            Paths.get(ops.joinPath(fromPath, item.name)),
                            Paths.get(ops.joinPath(toPath, item.name)),
                            StandardCopyOption.ATOMIC_MOVE
                        )
                    }

                }
            }
        } catch (e: Exception) {
            status = FileOpCode.FAIL
        } finally {
            getItems()
            transferList.clear()
        }

        return status
    }

    private fun copyDir(src: Path, dest: Path) {
        Files.walk(src).forEach {
            Files.copy(
                it, dest.resolve(src.relativize(it)),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    fun copyToDestination(): FileOpCode {

        var status = FileOpCode.SUCCESS

        val toPath = ops.getInternalPath()

        if (fromPath == toPath) {
            transferList.clear()
            return FileOpCode.SAME_PATH
        }

        try {

            var sourceFileStream: FileInputStream? = null
            var targetFileStream: FileOutputStream? = null
            for (item in transferList) {
                if (item.isSelected) {
                    if (item.isDir) {
                        copyDir(
                            Paths.get(ops.joinPath(fromPath, item.name)),
                            Paths.get(ops.joinPath(toPath, item.name))
                        )
                        continue
                    }

                    if (File(ops.joinPath(toPath, item.name)).exists()) {
                        status = FileOpCode.EXISTS
                        continue
                    }

                    sourceFileStream = FileInputStream(ops.joinPath(fromPath, item.name))
                    targetFileStream = FileOutputStream(ops.joinPath(toPath, item.name))
                    FileUtils.copy(sourceFileStream, targetFileStream)

                }
            }
            sourceFileStream?.close()
            targetFileStream?.close()
        } catch (e: Exception) {
            status = FileOpCode.FAIL
        } finally {
            transferList.clear()
            getItems()
        }
        return status
    }

    fun shareFile(): Boolean {

        val selectedFileName = itemList.find { it.isSelected }?.name

        if (selectedFileName.isNullOrEmpty()) {
            return false
        }

        val fileToShare = File(ops.joinPath(ops.getInternalPath(), selectedFileName))
        val fileUri = FileProvider.getUriForFile(
            application,
            application.applicationContext.packageName + ".provider",
            fileToShare
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = application.contentResolver.getType(fileUri)
        intent.putExtra(Intent.EXTRA_STREAM, fileUri)

        val chooser = Intent.createChooser(
            intent,
            application.getString(R.string.share_chooser_title)
        )
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(chooser)

        transferList.clear()
        clearSelection()
        getItems()

        return true
    }

    fun deleteItems() {
        viewModelScope.launch {
            for (item in itemList) {
                if (item.isSelected) {
                    ops.deleteFile(item)
                }
            }
            getItems()
        }
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

    fun setSelected(index: Int) {

        val item = ops.itemStateList[index]
        val isSelectedOld = item.isSelected

        item.isSelected = true

        ops.itemStateList[index] = item.copy(isSelected = !isSelectedOld)

        ops.baseItemList[index].isSelected = true

        when (item.isDir) {
            true -> selectedFolderCount.intValue += 1
            false -> selectedFileCount.intValue += 1
        }

    }

    fun setUnSelected(index: Int) {

        itemList[index].isSelected = false
        ops.itemStateList.clear()
        ops.itemStateList.addAll(ops.baseItemList)

        when (ops.baseItemList[index].isDir) {
            true -> selectedFolderCount.intValue -= 1
            false -> selectedFileCount.intValue -= 1
        }

        if (ops.baseItemList.all { !it.isSelected }) {
            appBarType.value = ActionBarType.NORMAL
            selectedFileCount.intValue = 0
            selectedFolderCount.intValue = 0
        }

    }

    fun clearSelection() {
        selectedFileCount.intValue = 0
        selectedFolderCount.intValue = 0
        ops.baseItemList.forEach { it.isSelected = false }
        ops.itemStateList.clear()
        ops.itemStateList.addAll(ops.baseItemList)
    }

    fun exportItems(uri: Uri) {
        viewModelScope.launch {
            for (item in transferList) {
                if (item.isSelected) {
                    ops.exportItems(uri, item)
                }
            }
            clearSelection()
            transferList.clear()
        }
    }

    fun setFromPath() {
        fromPath = ops.getInternalPath()
    }
}