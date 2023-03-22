package org.android.safespace.viewmodel

import android.app.Application
import android.net.Uri
import android.os.FileUtils
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
        if (internalPath.isEmpty()) {
            internalPath.add(dir)
        } else if (internalPath.last() != dir) {
            internalPath.add(dir)
        }
    }

    fun setPreviousPath() {
        if (internalPath.isNotEmpty()) internalPath.removeLast()
    }

    fun isRootDirectory(): Boolean {
        return internalPath.isEmpty()
    }

    fun importFile(uri: Uri, internalPath: String): Int {

        try {

            // create directory if not exists
            // createDir(internalPath, "")

            // source File from URI
            val sourceFileName = File(uri.path!!).name

            // byte array of source file
            val sourceFileStream = application.contentResolver.openInputStream(uri)

            // output stream for target file
            val targetFileStream =
                FileOutputStream(
                    File(
                        filesDirAbsolutePath + File.separator +
                                internalPath + File.separator +
                                sourceFileName
                    )
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
            val dirPath = if (newDirName == "") {
                filesDirAbsolutePath + File.separator + internalPath
            } else {
                filesDirAbsolutePath + File.separator + internalPath + File.separator + newDirName
            }

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

        val dirPath = File(application.filesDir.absolutePath + File.separator + internalPath)

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
            val absolutePath =
                filesDirAbsolutePath + File.separator + internalPath + File.separator

            val absoluteFilePathOld = File(absolutePath + file.name)

            val absoluteFilePathNew =
                File(absolutePath + newFileName + "." + file.name.split(".").last())

            absoluteFilePathOld.renameTo(absoluteFilePathNew)

        } catch (e: Exception) {
            return 0
        }

        return 1
    }

    fun deleteFile(file: FileItem, internalPath: String): Int {
        try {
            val fileToDelete = File(
                filesDirAbsolutePath + File.separator + internalPath +
                        File.separator + file.name
            )


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

}