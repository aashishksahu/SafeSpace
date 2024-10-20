package org.privacymatters.safespace.main

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
import org.privacymatters.safespace.utils.Utils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

enum class ActionBarType {
    NORMAL, LONG_PRESS, MOVE, COPY
}

class MainActivityViewModel(private val application: Application) : AndroidViewModel(application) {

    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

    var ops = DataManager

    private var transferList: ArrayList<Item> = arrayListOf()

    // 0: NormalActionBar, 1: LongPressActionBar, 2: MoveActionBar, 3: CopyActionBar
    var appBarType = mutableStateOf(ActionBarType.NORMAL)

    //    var scrollToPosition = ops.positionHistory
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
        val fileSortBy = sharedPref.getString(Constants.FILE_SORT_BY, Constants.NAME).toString()
        val fileSortOrder =
            sharedPref.getString(Constants.FILE_SORT_ORDER, Constants.ASC).toString()
        sortItems(fileSortBy, fileSortOrder)
    }

    fun sortItems(sortBy: String, sortOrder: String) {
        ops.getSortedItems(sortBy, sortOrder)

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
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }

                }
            }
        } catch (e: Exception) {
            Utils.exportToLog(application, "@DataManager.moveToDestination()", e)
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
            Utils.exportToLog(application, "@DataManager.copyToDestination()", e)
            status = FileOpCode.FAIL
        } finally {
            transferList.clear()
            getItems()
        }
        return status
    }

    fun shareFile(): Boolean {

        val selectedFileName = ops.itemListFlow.value.find { it.isSelected }?.name

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
            for (item in ops.itemListFlow.value) {
                if (item.isSelected) {
                    ops.deleteFile(item)
                }
            }
            clearSelection()
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

    fun setSelected(id: UUID) {

        ops.selectItem(id)

        val item = ops.itemListFlow.value.find { it.id == id }

        item?.let {
            when (it.isDir) {
                true -> selectedFolderCount.intValue += 1
                false -> selectedFileCount.intValue += 1
            }
            transferList.add(it)
        }

    }

    fun setUnSelected(id: UUID) {

        ops.unselectItem(id)

        val item = ops.itemListFlow.value.find { it.id == id }

        item?.let {
            when (it.isDir) {
                true -> selectedFolderCount.intValue -= 1
                false -> selectedFileCount.intValue -= 1
            }
        }
        transferList.removeIf { it.id == item?.id }

        if (ops.itemListFlow.value.all { !it.isSelected }) {
            clearSelection()
        }
    }


    fun clearSelection() {
        selectedFileCount.intValue = 0
        selectedFolderCount.intValue = 0
        appBarType.value = ActionBarType.NORMAL
        transferList.clear()

        ops.clearSelection()
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

    fun migrateFromRoot() {
        viewModelScope.launch {
            if (ops.migrateFromRoot() == FileOpCode.SUCCESS) {
                sharedPref.edit()
                    .putBoolean(Constants.MIGRATION_COMPLETE, true)
                    .apply()
            }
        }
    }

    fun isMigrationComplete(): Boolean {
        return sharedPref.getBoolean(Constants.MIGRATION_COMPLETE, false)
    }

    fun exportToLog(msg: String, e: Exception) {
        Utils.exportToLog(application, msg, e)
    }

    fun renameFile(newName: String): Boolean {

        val item = ops.itemListFlow.value.find { it.isSelected }

        if (item != null && ops.renameFile(item, newName)) {
            clearSelection()
            getItems()
            return true
        }
        return false
    }
}