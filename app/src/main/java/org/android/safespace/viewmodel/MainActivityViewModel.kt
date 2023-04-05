package org.android.safespace.viewmodel

import android.app.Application
import android.net.Uri
import android.os.FileUtils
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import org.android.safespace.lib.FileItem
import java.io.File
import java.io.FileOutputStream

class MainActivityViewModel(
    private val application: Application
) : ViewModel() {

    private var internalPath: ArrayList<String> = ArrayList()
    private var filesList: ArrayList<FileItem> = ArrayList()
    private var filesDirAbsolutePath: String = application.filesDir.absolutePath.toString()

    fun getInternalPath(): String {
        return internalPath.joinToString(File.separator)
    }

    fun setInternalPath(dir: String) {
        if (internalPath.isEmpty() || internalPath.last() != dir) {
            internalPath.add(dir)
        }
    }

    fun setGetPreviousPath(): String {
        val lastPath = internalPath.last()
        if (internalPath.isNotEmpty()) internalPath.removeLast()
        return lastPath
    }

    fun getLastDirectory(): String {
        return try {
            internalPath.last().toString()
        } catch (e: NoSuchElementException) {
            ""
        }
    }

    fun isRootDirectory(): Boolean {
        return internalPath.isEmpty()
    }

    fun joinPath(vararg pathList: String): String {

        return pathList.joinToString(File.separator).replace("//", "/")

    }

    fun importFile(uri: Uri, internalPath: String): Int {

        try {

            // create directory if not exists
            // createDir(internalPath, "")

            var sourceFileName = ""

            val cursor = application.contentResolver.query(
                uri, null, null, null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val colIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (colIndex >= 0) {
                        sourceFileName = it.getString(colIndex)
                    }
                }
            }

            // byte array of source file
            val sourceFileStream = application.contentResolver.openInputStream(uri)

            // output stream for target file
            val targetFileStream =
                FileOutputStream(
                    File(joinPath(filesDirAbsolutePath, internalPath, sourceFileName))
                )

            if (sourceFileStream != null) {
                FileUtils.copy(sourceFileStream, targetFileStream)
            } else {
                return -1
            }

            sourceFileStream.close()
            targetFileStream.close()

        } catch (e: Exception) {
            return -1
        }

        return 1
    }

    fun createDir(internalPath: String, newDirName: String): Int {

        try {
            val dirPath = joinPath(filesDirAbsolutePath, internalPath, newDirName)

            val newDir = File(dirPath)

            if (!newDir.exists()) {
                newDir.mkdirs()
            }

        } catch (e: FileSystemException) {
            return 0
        }

        return 1

    }

    fun getContents(internalPath: String): List<FileItem> {

        val dirPath = File(joinPath(application.filesDir.absolutePath, internalPath))

        val contents = dirPath.listFiles()

        filesList.clear()

        for (item in contents!!) {
            filesList.add(FileItem(item.name, item.length(), item.isDirectory))
        }

        // sort -> folders first -> ascending by name
        filesList.sortWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name })

        return filesList

    }

    fun renameFile(file: FileItem, internalPath: String, newFileName: String): Int {

        try {
            val absolutePath = joinPath(filesDirAbsolutePath, internalPath, File.separator)

            val absoluteFilePathOld = File(absolutePath + file.name)

            val absoluteFilePathNew = if ("." in file.name) {
                File(absolutePath + newFileName + "." + file.name.split(".").last())
            } else {
                File(absolutePath + newFileName)
            }

            absoluteFilePathOld.renameTo(absoluteFilePathNew)

        } catch (e: Exception) {
            return 0
        }

        return 1
    }

    fun deleteFile(file: FileItem, internalPath: String): Int {
        try {
            val fileToDelete = File(joinPath(filesDirAbsolutePath, internalPath, file.name))

            if (fileToDelete.exists()) {
                if (file.isDir) {
                    deleteDirectory(fileToDelete)
                }
                fileToDelete.delete()
            }

        } catch (e: Exception) {
            return 0
        }

        return 1

    }

    private fun deleteDirectory(fileToDelete: File): Int {
        try {
            val dirContents = fileToDelete.listFiles()
            for (file in dirContents!!) {
                if (file.isDirectory) {
                    deleteDirectory(File(file.absolutePath))
                } else {
                    file.delete()
                }
            }
            fileToDelete.delete()

        } catch (e: Exception) {
            return -1
        }
        return 1
    }

    fun setPath(currentPath: String) {
        val indexOfCurrentPath = internalPath.indexOf(currentPath)

        for (i in indexOfCurrentPath + 1 until internalPath.size) {
            internalPath.removeAt(i)
        }
    }

}