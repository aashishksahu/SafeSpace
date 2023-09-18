package org.privacymatters.safespace.lib

import android.app.Application
import android.net.Uri
import android.os.FileUtils
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


@Suppress("unused")
class Operations(private val application: Application) {

    companion object {
        private var internalPath: ArrayList<String> = ArrayList()
    }

    private var filesList: ArrayList<FileItem> = ArrayList()
    private var folderList: ArrayList<FolderItem> = ArrayList()

    var moveFileFrom: String? = null
    var moveFileTo: String? = null

    fun getFilesDir(): String {
        // root folder inside app files directory will be the first folder
        return application.filesDir.canonicalPath.toString() + File.separator + Constants.ROOT
    }

    fun getInternalPath(): String {
        return internalPath.joinToString(File.separator)
    }

    fun setInternalPath(dir: String) {
        if (internalPath.isEmpty() || internalPath.last() != dir) {
            internalPath.add(dir)
        }
    }

    fun setGetPreviousAndCurrentPath(): Pair<String, String> {
        val lastPath = internalPath.last()
        var currentPath = ""

        if (internalPath.isNotEmpty()) {
            internalPath.removeLast()
            currentPath = if (internalPath.size > 0) internalPath.last() else ""
        }
        return Pair(lastPath, currentPath)
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
                    File(joinPath(getFilesDir(), internalPath, sourceFileName))
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
            val dirPath = joinPath(getFilesDir(), internalPath, newDirName)

            val newDir = File(dirPath)

            if (!newDir.exists()) {
                newDir.mkdirs()
            }

        } catch (e: FileSystemException) {
            return 0
        }

        return 1

    }

    fun initRootDir(): Int {

        try {

            val newDir = File(getFilesDir())

            if (!newDir.exists()) {
                newDir.mkdirs()
            }

        } catch (e: FileSystemException) {
            return 0
        }

        return 1

    }

    fun getContents(internalPath: String): Pair<List<FileItem>, List<FolderItem>> {

        val dirPath = File(joinPath(getFilesDir(), internalPath))

        val contents = dirPath.listFiles()

        filesList.clear()
        folderList.clear()

        for (item in contents!!) {
            if (item.isDirectory) {

                val fileInsideFolder = item.listFiles()

                val fileCount = fileInsideFolder?.size ?: 0

                folderList.add(FolderItem(item.name, fileCount))

            } else {

                filesList.add(
                    FileItem(
                        item.name,
                        item.length(),
                        item.isDirectory,
                        item.lastModified()
                    )
                )
            }
        }

        // sort -> folders first -> ascending by name
        filesList.sortWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name })

        return Pair(filesList, folderList)

    }

    fun renameFile(file: FileItem, internalPath: String, newFileName: String): Int {

        try {
            val absolutePath = joinPath(getFilesDir(), internalPath, File.separator)

            val absoluteFilePathOld = File(absolutePath + file.name)

            val absoluteFilePathNew = File(absolutePath + newFileName)

            absoluteFilePathOld.renameTo(absoluteFilePathNew)

        } catch (e: Exception) {
            return 0
        }

        return 1
    }

    fun deleteFile(file: FileItem, internalPath: String): Int {
        try {
            val fileToDelete = File(joinPath(getFilesDir(), internalPath, file.name))

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

    fun deleteFolder(folder: FolderItem, internalPath: String): Int {
        try {
            val folderToDelete = File(joinPath(getFilesDir(), internalPath, folder.name))

            if (folderToDelete.exists()) {
                deleteDirectory(folderToDelete)
                folderToDelete.delete()
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

    fun moveFile(): Int {

        try {

            Files.move(
                Paths.get(moveFileFrom),
                Paths.get(moveFileTo),
                StandardCopyOption.REPLACE_EXISTING
            )

        } catch (e: Exception) {
            return -1
        } finally {

            moveFileFrom = null
            moveFileTo = null
        }
        return 1
    }

    fun copyFile(): Int {

        try {

            // byte array of source file
            val sourceFileStream = FileInputStream(moveFileFrom)

            // output stream for target file
            val targetFileStream = FileOutputStream(moveFileTo)

            FileUtils.copy(sourceFileStream, targetFileStream)

            sourceFileStream.close()
            targetFileStream.close()

        } catch (e: Exception) {
            return -1
        } finally {

            moveFileFrom = null
            moveFileTo = null
        }
        return 1
    }

    fun createTextNote(noteName: String): String {

        val filePath = joinPath(getFilesDir(), getInternalPath(), noteName)

        val newTextNote = File(filePath)
        val result = newTextNote.createNewFile()

        return if (result) {
            filePath
        } else {
            Constants.FILE_EXIST
        }

    }

    fun exportItems(
        exportUri: Uri,
        selectedItem: FileItem
    ): Boolean {

        try {
            val fileToExport =
                File(getFilesDir() + File.separator + getInternalPath() + File.separator + selectedItem.name)

            val fis = FileInputStream(fileToExport)

            val directory = DocumentFile.fromTreeUri(application, exportUri)
            val file = directory!!.createFile("*", selectedItem.name)
            val pfd = application.contentResolver.openFileDescriptor(file!!.uri, "w")
            val fos = FileOutputStream(pfd!!.fileDescriptor)

            FileUtils.copy(fis, fos)

            fos.close()
            pfd.close()


        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun isPreviousRootDirectory(): Boolean {
        if (internalPath.size == 0) {
            return true
        }
        return false
    }

    fun exportBackup(exportUri: Uri): Int {

        try {
            val backupName =
                "SafeSpace-" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                    .format(System.currentTimeMillis()) + ".zip"

            val directory = DocumentFile.fromTreeUri(application, exportUri)
            val file = directory!!.createFile("application/zip", backupName)
            val pfd = application.contentResolver.openFileDescriptor(file!!.uri, "w")

            val inputDirectory = File(getFilesDir())

            ZipOutputStream(BufferedOutputStream(FileOutputStream(pfd!!.fileDescriptor))).use { zos ->

                inputDirectory.walkTopDown().forEach { file ->
                    val zipFileName =
                        file.absolutePath.removePrefix(inputDirectory.absolutePath)
                            .removePrefix("/")
                    val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")

                    zos.putNextEntry(entry)

                    if (file.isFile) {
                        file.inputStream().use { fis -> fis.copyTo(zos) }
                    }
                }

            }
            pfd.close()

        } catch (e: IOException) {
//            e.printStackTrace()
            return if (e.message.toString().lowercase().contains("no space left")) {
                4
            } else {
                1
            }
        }
        return 0
    }

    @Throws(SecurityException::class)
    fun importBackup(backupUri: Uri): Int {

        try {

            // byte array of source file
            val sourceFileStream = application.contentResolver.openInputStream(backupUri)

            val zis = ZipInputStream(sourceFileStream)

            var zipEntry = zis.nextEntry

            while (zipEntry != null) {
                val newFile = File(getFilesDir(), zipEntry.name)

                // https://support.google.com/faqs/answer/9294009
                val canonicalPath = newFile.canonicalPath

                if (!canonicalPath.startsWith(getFilesDir())) {
                    throw SecurityException()
                } else {
                    if (zipEntry.isDirectory) {
                        if (!newFile.isDirectory && !newFile.mkdirs()) {
                            throw IOException("Failed to create directory $newFile")
                        }
                    } else {
                        // fix for Windows-created archives
                        val parent = newFile.parentFile
                        if (parent != null) {
                            if (!parent.isDirectory && !parent.mkdirs()) {
                                throw IOException("Failed to create directory $parent")
                            }
                        }

                        // write file content
                        val fos = FileOutputStream(newFile)

                        zis.copyTo(fos)

                        fos.close()
                    }
                }
                zipEntry = zis.nextEntry
            }

            zis.closeEntry()
            zis.close()
            sourceFileStream?.close()
        } catch (exc: IOException) {
            return if (exc.message.toString().lowercase().contains("no space left")) {
                4
            } else {
                1
            }
        } catch (sec: SecurityException) {
//            sec.printStackTrace()
            return 1
        }
        return 0
    }


    @Suppress("Unused")
    private fun recursiveDirectoryRead(
        path: String,
        pFilesArray: ArrayList<String>
    ): ArrayList<String> {

        var filesArray = pFilesArray

        val directoryContents = File(path).listFiles()

        for (item in directoryContents!!) {
            val filePath = joinPath(path, item.name)
            if (item.isDirectory) {
                filesArray = recursiveDirectoryRead(filePath, filesArray)
            } else {
                filesArray.add(filePath)
            }
        }

        return filesArray

    }

}