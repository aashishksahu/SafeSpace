package org.privacymatters.safespace.experimental.mainn

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.fileManager.Utils
import org.privacymatters.safespace.lib.utils.Constants
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

    fun ready(app: Application): Int {
        application = app

        // initialize at first run of app. Sets the root directory
        try {
            val rootDir = File(getFilesDir())
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

    fun getItems(): ArrayList<Item> {

        val dirPath = File(getInternalPath())
        var icon: Bitmap?
        var iconDrawable: Int = -1
        val iconSize = 256
        var fileCount = ""
        val contents = dirPath.listFiles()
        val tempItemsList = arrayListOf<Item>()

        contents?.let {
            for (content in it) {
                icon = null

                if (content.isDirectory) {

                    // File count
                    val fileInsideFolder = content.listFiles()
                    val count = fileInsideFolder?.size
                    fileCount = when (count) {
                        0 -> {
                            "0 " + application.getString(R.string.items)
                        }

                        1 -> {
                            "1 " + application.getString(R.string.item)
                        }

                        else -> {
                            count.toString() + " " + application.getString(R.string.items)
                        }
                    }

                    // Icon
                    iconDrawable = R.drawable.folder_36dp
                } else {

                    val file = File(content.canonicalPath)

                    // Generate thumbnail based on file type
                    if (Utils.getFileType(content.name) == Constants.AUDIO_TYPE) {
                        try {
                            icon = ThumbnailUtils.extractThumbnail(
                                ThumbnailUtils.createAudioThumbnail(
                                    file,
                                    Size(iconSize, iconSize),
                                    null
                                ),
                                iconSize,
                                iconSize
                            )
                        } catch (ex: IOException) {
                            iconDrawable = R.drawable.music_note_white_36dp
                        }
                    } else if (Utils.getFileType(content.name) == Constants.VIDEO_TYPE) {
                        try {
                            icon = ThumbnailUtils.extractThumbnail(
                                ThumbnailUtils.createVideoThumbnail(
                                    file,
                                    Size(iconSize, iconSize),
                                    null
                                ),
                                iconSize,
                                iconSize
                            )
                        } catch (ex: IOException) {
                            iconDrawable = R.drawable.video_file_white_36dp
                        }
                    } else if (Utils.getFileType(content.name) == Constants.IMAGE_TYPE) {
                        try {
                            icon = ThumbnailUtils.extractThumbnail(
                                ThumbnailUtils.createImageThumbnail(
                                    file,
                                    Size(iconSize, iconSize),
                                    null
                                ),
                                iconSize,
                                iconSize
                            )
                        } catch (ex: IOException) {
                            iconDrawable = R.drawable.image_white_36dp
                        }
                    } else {
                        iconDrawable = R.drawable.description_white_36dp
                    }
                }
                tempItemsList.add(
                    Item(
                        icon = icon,
                        iconDrawable = iconDrawable,
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

        // sort -> folders first -> ascending by name
        tempItemsList.sortWith(compareByDescending<Item> { it.isDir }.thenBy { it.name })

        return tempItemsList
    }

    @Throws(SecurityException::class)
    fun extractZip(filePath: String): Boolean {
        try {

            val currentDir = getInternalPath()

            // byte array of source file
            val sourceFileStream = FileInputStream(filePath)

            val zis = ZipInputStream(sourceFileStream)

            var zipEntry = zis.nextEntry

            while (zipEntry != null) {
                val newFile = File(currentDir, zipEntry.name)

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

    suspend fun importFile(uri: Uri, internalPath: String): Boolean =
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

                var targetFile: File? = null

                var i = 1

                while (i != -1) {
                    val (nameOnly, ext) = Utils.getFileNameAndExtension(sourceFileName)

                    val duplicateFile: String = if (ext == Constants.BIN) {
                        "$nameOnly($i)"
                    } else {
                        "$nameOnly($i).$ext"
                    }

                    targetFile = File(joinPath(internalPath, duplicateFile))

                    if (targetFile.isFile) {
                        i += 1
                    } else {
                        i = -1
                    }

                }

                // output stream for target file
                val targetFileStream = targetFile?.let {
                    FileOutputStream(it)
                }

                if (sourceFileStream != null && targetFileStream != null) {
                    copyFileWithProgressNotifications(
                        sourceFileName, sourceFileStream,
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

    private suspend fun copyFileWithProgressNotifications(
        sourceFileName: String,
        sourceFileStream: InputStream,
        targetFileStream: FileOutputStream,
        type: FileTransferNotification.NotificationType,
        context: Context
    ) {
        val uniqueNotificationId = sourceFileName.hashCode()
        val fileTransferNotification = FileTransferNotification(context, uniqueNotificationId)
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

}
