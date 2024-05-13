package org.privacymatters.safespace.experimental.main

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.privacymatters.safespace.R
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.Utils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

object DataManager {

    // keep the "" , helps in loading the breadcrumbs initially
    var internalPath: ArrayList<String> = arrayListOf(Constants.ROOT)
    private lateinit var application: Application

    val itemStateList: SnapshotStateList<Item> = mutableStateListOf()
    var baseItemList: List<Item> = itemStateList

    var positionHistory = mutableIntStateOf(0)

    fun ready(app: Application): Int {
        application = app

        // initialize at first run of app. Sets the root directory
        try {
            val rootDir = File(joinPath(getFilesDir(), Constants.ROOT))
            if (!rootDir.exists()) {
                rootDir.mkdirs()
            }
        } catch (e: FileSystemException) {
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
                        name = content.name,
                        size = Utils.getSize(content.length()),
                        isDir = content.isDirectory,
                        itemCount = fileCount,
                        lastModified = Utils.convertLongToDate(content.lastModified()),
                        isSelected = false
                    )
                )
            }
        }

        return tempItemsList

    }

    fun getSortedItems(sortBy: String, sortOrder: String) {

        baseItemList = getItems()

        // Ascending or descending
        when (sortOrder) {
            Constants.ASC -> {
                // name, date or size
                baseItemList = when (sortBy) {
                    Constants.SIZE -> baseItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenBy { it.size })

                    Constants.DATE -> baseItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenBy { it.lastModified })

                    else -> baseItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenComparing { o1, o2 ->
                            naturalCompareAscending(o1, o2)
                        })
                }
            }

            Constants.DESC -> {
                // name, date or size
                baseItemList = when (sortBy) {
                    Constants.SIZE -> baseItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenByDescending { it.size })

                    Constants.DATE -> baseItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenByDescending { it.lastModified })

                    else -> baseItemList.sortedWith(compareByDescending<Item> { it.isDir }
                        .thenComparing { o1, o2 ->
                            naturalCompareDescending(o1, o2)
                        })

                }
            }
        }
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
            Log.e(Constants.TAG_ERROR, "@ DataManager.extractZip() ", e)
            return false
        } catch (sec: SecurityException) {
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
            exception.printStackTrace()
            fileTransferNotification.showFailureNotification(sourceFileName, exception)
        }
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
