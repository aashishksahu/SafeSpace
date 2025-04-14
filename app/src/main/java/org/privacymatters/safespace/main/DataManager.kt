package org.privacymatters.safespace.main

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.privacymatters.safespace.R
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.Utils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class FileOpCode {
    SUCCESS, EXISTS, FAIL, SAME_PATH, NO_SPACE
}

object DataManager {

    // keep the "" , helps in loading the breadcrumbs initially
    var internalPath: ArrayList<String> = arrayListOf(Constants.ROOT)
    private lateinit var application: Application

    private val privateItemList = MutableStateFlow<List<Item>>(emptyList())
    val itemListFlow = privateItemList.asStateFlow()

    var positionHistory = mutableIntStateOf(-1)

    var lockItem = false // if true, navigating away from the activity will show passcode screen

    var openedItem: Item? = null

    fun ready(app: Application): Int {
        application = app

        // initialize at first run of app. Sets the root directory
        try {
            val rootDir = File(joinPath(getFilesDir(), Constants.ROOT))
            if (!rootDir.exists()) {
                rootDir.mkdirs()
            }
        } catch (e: FileSystemException) {
            Utils.exportToLog(application, "@ DataManager.ready() ", e)
            Log.e(Constants.TAG_ERROR, "@ DataManager.ready() ", e)
            return 0
        }

        return 1
    }

    private fun getFilesDir(): String {
        // root folder inside app files directory will be the first folder
        return application.filesDir.canonicalPath.toString() + File.separator
    }

    fun joinPath(vararg pathList: String): String {
        return pathList.joinToString(File.separator).replace("//", "/")
    }

    fun getInternalPath(): String {
        return joinPath(getFilesDir(), internalPath.joinToString(File.separator))
    }

    fun getInternalPathList(): List<String> {
        return internalPath
    }

    private fun getItems(): ArrayList<Item> {

        val dirPath = File(getInternalPath())
        var fileCount = ""
        val contents = dirPath.listFiles()
        val tempItemsList = arrayListOf<Item>()

        contents?.let {
            for (content in it) {

                if (content.isDirectory) {
                    // File count
                    val fileInsideFolder = content.listFiles()
                    val count = fileInsideFolder?.size
                    fileCount = when (count) {
                        0 -> "0 " + application.getString(R.string.items)
                        1 -> "1 " + application.getString(R.string.item)
                        else -> count.toString() + " " + application.getString(R.string.items)
                    }

                }

                tempItemsList.add(
                    Item(
                        id = UUID.randomUUID(),
                        name = content.name,
                        size = content.length(),
                        isDir = content.isDirectory,
                        itemCount = fileCount,
                        lastModified = content.lastModified(),
                        isSelected = false
                    )
                )
            }
        }

        return tempItemsList

    }

    fun getSortedItems(sortBy: String, sortOrder: String) {

        var tempItemList: List<Item> = getItems()

        // Ascending or descending
        when (sortOrder) {
            Constants.ASC -> {
                // name, date or size
                tempItemList = when (sortBy) {
                    Constants.SIZE -> tempItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenBy { it.size })

                    Constants.DATE -> tempItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenBy { it.lastModified })

                    else -> tempItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenComparing { o1, o2 ->
                            naturalCompareAscending(o1, o2)
                        })
                }
            }

            Constants.DESC -> {
                // name, date or size
                tempItemList = when (sortBy) {
                    Constants.SIZE -> tempItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenByDescending { it.size })

                    Constants.DATE -> tempItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenByDescending { it.lastModified })

                    else -> tempItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenComparing { o1, o2 ->
                            naturalCompareDescending(o1, o2)
                        })

                }
            }
        }

        privateItemList.value = tempItemList
    }

    @Throws(SecurityException::class)
    fun extractZip(filePath: String): Boolean {

        try {
            var (zipDir, _) = Utils.getFileNameAndExtension(filePath)

            zipDir = checkDuplicate(File(zipDir).name).canonicalPath

            // byte array of source file
            val sourceFileStream = FileInputStream(filePath)

            val zis = ZipInputStream(sourceFileStream)

            var zipEntry = zis.nextEntry

            while (zipEntry != null) {
                val newFile = File(zipDir, zipEntry.name)

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
            sourceFileStream.close()
        } catch (e: IOException) {
            Utils.exportToLog(application, "@ DataManager.extractZip() ", e)
            Log.e(Constants.TAG_ERROR, "@ DataManager.extractZip() ", e)
            return false
        } catch (sec: SecurityException) {
            Utils.exportToLog(application, "@ DataManager.extractZip() ", sec)
            return false

        }
        return true
    }

    suspend fun importFile(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
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

                val targetFile = checkDuplicate(sourceFileName)

                // output stream for target file
                val targetFileStream = FileOutputStream(targetFile)

                if (sourceFileStream != null) {
                    copyFileWithProgressNotifications(
                        sourceFileName,
                        sourceFileStream,
                        targetFileStream,
                        FileTransferNotification.NotificationType.Import,
                        application.applicationContext,
                    )

                } else {
                    throw Exception("sourceFileStream or targetFileStream is null")
                }

                sourceFileStream.close()
                targetFileStream.close()

            } catch (e: Exception) {
                Utils.exportToLog(application, "@ DataManager.importFile() ", e)
                Log.e(Constants.TAG_ERROR, "@DataManager.importFile() ", e)
            }

            return@withContext true
        }


    private fun checkDuplicate(sourceFileName: String): File {

        var targetFile = File(joinPath(getInternalPath(), sourceFileName))
        val (nameOnly, ext) = Utils.getFileNameAndExtension(targetFile.name)

        if (targetFile.exists()) {
            var i = 1

            while (true) {

                val duplicateFile: String = if (ext == Constants.BIN) {
                    "$nameOnly($i)"
                } else {
                    "$nameOnly($i).$ext"
                }

                targetFile = File(joinPath(getInternalPath(), duplicateFile))

                if (targetFile.exists()) {
                    i += 1
                } else {
                    break
                }
            }
        }
        return targetFile
    }

    private suspend fun copyFileWithProgressNotifications(
        sourceFileName: String,
        sourceFileStream: InputStream,
        targetFileStream: FileOutputStream,
        type: FileTransferNotification.NotificationType,
        context: Context
    ) {
        val uniqueNotificationId = sourceFileName.hashCode()
        val fileTransferNotification =
            FileTransferNotification(context, uniqueNotificationId)
        try {
            FileHelper.copyFileWithProgress(sourceFileStream, targetFileStream)
                .collect { progress ->
                    fileTransferNotification.showProgressNotification(
                        fileName = sourceFileName,
                        progress = progress,
                        type = type
                    )
                }
            fileTransferNotification.showSuccessNotification(
                fileName = sourceFileName,
                type = type
            )

        } catch (exception: Exception) {
            Utils.exportToLog(
                application,
                "@ DataManager.copyFileWithProgressNotification() ",
                exception
            )
            exception.printStackTrace()
            fileTransferNotification.showFailureNotification(sourceFileName, exception)
        }
    }

    fun deleteFile(item: Item) {
        try {
            val fileToDelete = File(joinPath(getInternalPath(), item.name))

            if (fileToDelete.exists()) {
                if (item.isDir) {
                    deleteDirectory(fileToDelete)
                }
                fileToDelete.delete()
            }

        } catch (e: Exception) {
            Utils.exportToLog(application, "@ DataManager.deleteFile() ", e)
            Log.e(Constants.TAG_ERROR, "@DataManager.deleteFile() ", e)
        }
    }

    private fun deleteDirectory(dir: File) {
        try {
            val dirContents = dir.listFiles()
            for (file in dirContents!!) {
                if (file.isDirectory) {
                    deleteDirectory(File(file.absolutePath))
                } else {
                    file.delete()
                }
            }
            dir.delete()

        } catch (e: Exception) {
            Utils.exportToLog(application, "@ DataManager.deleteDirectory() ", e)
            Log.e(Constants.TAG_ERROR, "@DataManager.deleteDirectory() ", e)
        }
    }

    fun exportItems(
        exportDir: DocumentFile?,
        fileToExport: File
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            try {

                if (fileToExport.isDirectory) {
                    exportDir?.createDirectory(fileToExport.name)

                    val subFolder = exportDir?.listFiles()
                        ?.find { it.isDirectory && it.name == fileToExport.name }

                    val filesInFolder = fileToExport.listFiles()

                    filesInFolder?.forEach { file ->
                        exportItems(subFolder, file)
                    }
                } else {
                    exportDir?.let {

                        val createdFile = it.createFile("*", fileToExport.name)
                        val fis = FileInputStream(fileToExport)

                        val pfd = application.contentResolver
                            .openFileDescriptor(createdFile?.uri!!, "w")

                        val fos = FileOutputStream(pfd!!.fileDescriptor)

                        copyFileWithProgressNotifications(
                            fileToExport.name, fis, fos,
                            FileTransferNotification.NotificationType.Export,
                            application.applicationContext
                        )
                        fos.close()
                        pfd.close()
                    }
                }


            } catch (e: Exception) {
                Utils.exportToLog(application, "@ DataManager.exportItems() ", e)
                Log.e(Constants.TAG_ERROR, "@DataManager.exportItems() ", e)
            }
        }
    }

    @Throws(SecurityException::class)
    fun importBackup(backupUri: Uri): Any {
        val uniqueNotificationId = backupUri.toString().hashCode()
        val fileTransferNotification =
            FileTransferNotification(application.applicationContext, uniqueNotificationId)
        val notificationTag = "Backup"

        try {
            // byte array of source file
            val sourceFileStream = application.contentResolver.openInputStream(backupUri)
                ?: throw IOException("Failed to open input stream")

            val zis = ZipInputStream(sourceFileStream.buffered())

            var zipEntry = zis.nextEntry
            var totalEntryCount = 1 // to avoid division by zero error
            var entryCount = 0

            while (zipEntry != null) {
                val newFile = File(getFilesDir(), zipEntry.name)
                // https://support.google.com/faqs/answer/9294009
                val canonicalPath = newFile.canonicalPath

                // Get total entry count from metadata.txt
                if (zipEntry.name == Constants.BACKUP_METADATA) {
                    val metadata = zis.readBytes().toString(StandardCharsets.UTF_8).split("\n")
                    totalEntryCount = metadata[0].substringAfter("=").toInt()
                    zipEntry = zis.nextEntry
                    continue
                }

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
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }

                entryCount++
                val progress = (entryCount * 100 / totalEntryCount)
                fileTransferNotification.showProgressNotification(
                    notificationTag, progress.coerceAtMost(100),
                    FileTransferNotification.NotificationType.Import
                )

                zipEntry = zis.nextEntry
            }

            zis.closeEntry()
            zis.close()
            sourceFileStream.close()

            fileTransferNotification.showSuccessNotification(
                notificationTag,
                FileTransferNotification.NotificationType.Import,
                true
            )
            return FileOpCode.SUCCESS

        } catch (e: IOException) {
            Utils.exportToLog(application, "@ DataManager.importBackup() ", e)
            Log.e(Constants.TAG_ERROR, "@DataManager.importBackup() ", e)
            fileTransferNotification.showFailureNotification(notificationTag, e)
            return if (e.message.toString().lowercase().contains("no space left")) {
                FileOpCode.NO_SPACE
            } else {
                FileOpCode.FAIL
            }
        } catch (e: SecurityException) {
            Utils.exportToLog(application, "@ DataManager.importBackup() ", e)
            Log.e(Constants.TAG_ERROR, "@DataManager.importBackup() ", e)
            fileTransferNotification.showFailureNotification(notificationTag, e)
            return FileOpCode.FAIL
        }
    }

    fun exportBackup(exportUri: Uri): Any {
        val notificationTag = "Backup"
        val backupName = "SafeSpace-" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".zip"
        val uniqueNotificationId = backupName.hashCode()
        val fileTransferNotification =
            FileTransferNotification(application.applicationContext, uniqueNotificationId)

        try {
            val directory = DocumentFile.fromTreeUri(application, exportUri)
            val file = directory!!.createFile("application/zip", backupName)
            val pfd = application.contentResolver.openFileDescriptor(file!!.uri, "w")

            val inputDirectory = File(getFilesDir() + Constants.ROOT)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(pfd!!.fileDescriptor))).use { zos ->
                var totalFiles = 0
                var processedFiles = 0

                // Count total files
                inputDirectory.walkTopDown().forEach { if (it.isFile) totalFiles++ }

                /*
                 * Add a metadata.txt file to the ZIP archive
                 * This file will contain the total number of files in the backup and the time of backup.
                 *
                 * Due to the nature of ZipInputStream, it is not possible to count the total number
                 * of entries in the ZIP file without reading through each entry. Once we've read the
                 * entries to count them, we cannot read them again for extraction.
                 *
                 * To work around this, we first write the metadata to the ZIP file, then write the
                 * actual files. This allows us to have the total number of files available for progress
                 * notifications during the backup import.
                 */
                val metadataEntry = ZipEntry(Constants.BACKUP_METADATA)
                zos.putNextEntry(metadataEntry)
                zos.write("totalFiles=$totalFiles".toByteArray())
                zos.write("\n".toByteArray())
                zos.write("backupTime=${System.currentTimeMillis()}".toByteArray())
                zos.closeEntry()

                // Write files to zip
                inputDirectory.walkTopDown().forEach { file ->
                    val zipFileName =
                        Constants.ROOT + file.absolutePath.removePrefix(inputDirectory.absolutePath)
                    val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                    zos.putNextEntry(entry)

                    if (file.isFile) {
                        file.inputStream().use { fis -> fis.copyTo(zos) }
                        processedFiles++
                        val progress = (processedFiles * 100 / totalFiles)
                        fileTransferNotification.showProgressNotification(
                            notificationTag, progress,
                            FileTransferNotification.NotificationType.Export
                        )
                    }
                }
            }
            pfd.close()

            fileTransferNotification.showSuccessNotification(
                notificationTag,
                FileTransferNotification.NotificationType.Export,
                isBackupFile = true
            )
            return FileOpCode.SUCCESS
        } catch (e: IOException) {
            Utils.exportToLog(application, "@ DataManager.exportBackup() ", e)
            fileTransferNotification.showFailureNotification(notificationTag, e)
            return if (e.message.toString().lowercase().contains("no space left")) {
                FileOpCode.NO_SPACE
            } else {
                FileOpCode.FAIL
            }
        }
    }

    fun selectItem(id: UUID) = privateItemList.update {
        it.map { item ->
            if (item.id == id) item.copy(isSelected = true)
            else item
        }
    }

    fun unselectItem(id: UUID) = privateItemList.update {
        it.map { item ->
            if (item.id == id) item.copy(isSelected = false)
            else item
        }
    }

    fun clearSelection() = privateItemList.update {
        it.map { item ->
            item.copy(isSelected = false)
        }
    }

    fun migrateFromRoot(): Any {

        try {

            val oldRoot = File(getFilesDir() + "root")

            oldRoot.listFiles().let {
                if (it != null) {
                    if (it.isEmpty()) {
                        return FileOpCode.SUCCESS
                    }
                }
            }

            oldRoot.walkTopDown().forEach { file ->

                val sourcePath = file.absolutePath
                val targetPath = file.absolutePath.replaceFirst("root", Constants.ROOT)

                if (file.isDirectory) {
                    File(targetPath).mkdirs()
                } else {
                    Files.move(
                        Paths.get(sourcePath),
                        Paths.get(targetPath),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        } catch (e: Exception) {
            Utils.exportToLog(application, "@ DataManager.migrateFromRoot() ", e)
            Log.e(Constants.TAG_ERROR, "@DataManager.migrateFromRoot() ", e)
            return FileOpCode.FAIL
        } finally {
            File(joinPath(getFilesDir(), "root")).deleteRecursively()
        }

        return FileOpCode.SUCCESS
    }

    fun renameFile(item: Item, newName: String): Boolean {

        try {
            val absolutePath = joinPath(getInternalPath(), File.separator)

            val absoluteFilePathOld = File(absolutePath + item.name)

            val newNameWithExt = if (item.isDir) {
                newName
            } else {
                "$newName.${item.name.substringAfterLast('.')}"
            }

            val absoluteFilePathNew = File(absolutePath + newNameWithExt)

            absoluteFilePathOld.renameTo(absoluteFilePathNew)

        } catch (e: Exception) {
            Utils.exportToLog(application, "@DataManager.renameFile()", e)
            return false
        }

        return true
    }

    /*
        private fun getThumbsDir(): String {
            // thumbnails folder
            return joinPath(
                application.filesDir.canonicalPath.toString(),
                Constants.THUMBS
            ) + File.separator
        }

       private fun saveThumbnailCache(icon: File, file: File) {
           CoroutineScope(Dispatchers.IO).launch {
               val iconSize = 64
               try {
                   val bmp = when (Utils.getFileType(file.name)) {
                       Constants.AUDIO_TYPE -> {
                           ThumbnailUtils.extractThumbnail(
                               ThumbnailUtils.createAudioThumbnail(
                                   file,
                                   Size(iconSize, iconSize),
                                   null
                               ),
                               iconSize,
                               iconSize
                           )
                       }

                       Constants.VIDEO_TYPE -> {
                           ThumbnailUtils.extractThumbnail(
                               ThumbnailUtils.createVideoThumbnail(
                                   file,
                                   Size(iconSize, iconSize),
                                   null
                               ),
                               iconSize,
                               iconSize
                           )
                       }

                       Constants.IMAGE_TYPE -> {
                           ThumbnailUtils.extractThumbnail(
                               ThumbnailUtils.createImageThumbnail(
                                   file,
                                   Size(iconSize, iconSize),
                                   null
                               ),
                               iconSize,
                               iconSize
                           )
                       }

                       else -> null
                   }

                   bmp?.let {
                       if (!icon.exists()) {
                           val path = Utils.getPathAndFileName(icon.canonicalPath).first
                           Files.createDirectories(Paths.get(path))
                           icon.createNewFile()
                       }
                       FileOutputStream(icon).use { out ->
                           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                               it.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, out)
                           } else {
                               @Suppress("DEPRECATION")
                               it.compress(Bitmap.CompressFormat.WEBP, 50, out)
                           }
                       }
                   }
               } catch (e: Exception) {
                   Log.e(Constants.TAG_ERROR, "@ DataManager.saveThumbnailCache() ", e)
               }
           }
       }
       */

}
