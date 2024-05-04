package org.privacymatters.safespace.experimental.mainn

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.util.Log
import android.util.Size
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.fileManager.Utils
import org.privacymatters.safespace.lib.utils.Constants
import java.io.File
import java.io.IOException

object DataManager {

    var internalPath: ArrayList<String> = arrayListOf()
    private lateinit var application: Application

    fun ready(app: Application): Int {
        application = app

        // initialize at first run of app. Sets the root directory
        try {
            val rootDir = File(getFilesDir())
            if (!rootDir.exists()) {
                rootDir.mkdirs()
            }
//            internalPath.add(Constants.ROOT)
        } catch (e: FileSystemException) {
            Log.e(Constants.TAG_ERROR, "@ DataManager.ready() ", e)
            return 0
        }

        return 1
    }

    private fun getFilesDir(): String {
        // root folder inside app files directory will be the first folder
        return application.filesDir.canonicalPath.toString() + File.separator + Constants.ROOT
    }

    private fun joinPath(vararg pathList: String): String {

        return pathList.joinToString(File.separator).replace("//", "/")

    }

    private fun getInternalPath(): String {
        return internalPath.joinToString(File.separator)
    }

    fun getItems(): ArrayList<Item> {

        val dirPath = File(joinPath(getFilesDir(), getInternalPath()))
        var icon: Bitmap
        val iconSize = 256
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
                        0 -> {
                            "0" + application.getString(R.string.items)
                        }

                        1 -> {
                            "1" + application.getString(R.string.item)
                        }

                        else -> {
                            count.toString() + application.getString(R.string.items)
                        }
                    }

                    // Icon
                    icon =
                        BitmapFactory.decodeResource(application.resources, R.drawable.folder_36dp)
                } else {

                    val file = File(content.canonicalPath)

                    // Generate thumbnail based on file type
                    if (Utils.getFileType(content.name) == Constants.AUDIO_TYPE) {
                        icon = try {
                            ThumbnailUtils.extractThumbnail(
                                ThumbnailUtils.createAudioThumbnail(
                                    file,
                                    Size(iconSize, iconSize),
                                    null
                                ),
                                iconSize,
                                iconSize
                            )
                        } catch (ex: IOException) {
                            BitmapFactory.decodeResource(
                                application.resources,
                                R.drawable.music_note_white_36dp
                            )
                        }
                    } else if (Utils.getFileType(content.name) == Constants.VIDEO_TYPE) {
                        icon = try {
                            ThumbnailUtils.extractThumbnail(
                                ThumbnailUtils.createVideoThumbnail(
                                    file,
                                    Size(iconSize, iconSize),
                                    null
                                ),
                                iconSize,
                                iconSize
                            )
                        } catch (ex: IOException) {
                            BitmapFactory.decodeResource(
                                application.resources,
                                R.drawable.video_file_white_36dp
                            )
                        }
                    } else if (Utils.getFileType(content.name) == Constants.IMAGE_TYPE) {
                        icon = try {
                            ThumbnailUtils.extractThumbnail(
                                ThumbnailUtils.createImageThumbnail(
                                    file,
                                    Size(iconSize, iconSize),
                                    null
                                ),
                                iconSize,
                                iconSize
                            )
                        } catch (ex: IOException) {
                            BitmapFactory.decodeResource(
                                application.resources,
                                R.drawable.image_white_36dp
                            )
                        }
                    } else {
                        icon = BitmapFactory.decodeResource(
                            application.resources,
                            R.drawable.description_white_36dp
                        )
                    }
                }
                tempItemsList.add(
                    Item(
                        icon = icon,
                        name = content.name,
                        size = Utils.getSize(content.length()),
                        isDir = content.isDirectory,
                        itemCount = fileCount,
                        lastModified = Utils.convertLongToTime(content.lastModified()),
                        isSelected = false
                    )
                )
            }
        }

        // sort -> folders first -> ascending by name
        tempItemsList.sortWith(compareByDescending<Item> { it.isDir }.thenBy { it.name })

        return tempItemsList
    }

}
