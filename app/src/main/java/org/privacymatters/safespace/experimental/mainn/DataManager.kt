package org.privacymatters.safespace.experimental.mainn

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.CancellationSignal
import android.util.Size
import androidx.lifecycle.MutableLiveData
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.fileManager.Utils
import org.privacymatters.safespace.lib.utils.Constants
import java.io.File
import java.io.IOException

object DataManager {

    var internalPath: MutableLiveData<ArrayList<String>> = MutableLiveData()
    private lateinit var application: Application
    fun init(app: Application): DataManager {
        application = app

        val tempInternalPath = arrayListOf<String>()
        tempInternalPath.add(Constants.ROOT)
        internalPath.value = tempInternalPath

        return this
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

    private fun getFilesDir(): String {
        // root folder inside app files directory will be the first folder
        return application.filesDir.canonicalPath.toString() + File.separator + Constants.ROOT
    }

    private fun joinPath(vararg pathList: String): String {

        return pathList.joinToString(File.separator).replace("//", "/")

    }

    private fun getInternalPath(): String {
        return internalPath.value?.joinToString(File.separator) ?: Constants.ROOT
    }

    fun getItems(): ArrayList<Item> {

        val dirPath = File(joinPath(getFilesDir(), getInternalPath()))
        var icon: Bitmap
        val iconSize = 64
        var fileCount = ""
        val contents = dirPath.listFiles()

        val itemList = arrayListOf<Item>()


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
                            ThumbnailUtils.createAudioThumbnail(
                                file,
                                Size(iconSize, iconSize),
                                CancellationSignal()
                            )
                        } catch (ex: IOException) {
                            BitmapFactory.decodeResource(
                                application.resources,
                                R.drawable.music_note_white_36dp
                            )
                        }
                    } else if (Utils.getFileType(content.name) == Constants.VIDEO_TYPE) {
                        icon = try {
                            ThumbnailUtils.createVideoThumbnail(
                                file,
                                Size(iconSize, iconSize),
                                CancellationSignal()
                            )
                        } catch (ex: IOException) {
                            BitmapFactory.decodeResource(
                                application.resources,
                                R.drawable.video_file_white_36dp
                            )
                        }
                    } else if (Utils.getFileType(content.name) == Constants.IMAGE_TYPE) {
                        icon = try {
                            ThumbnailUtils.createImageThumbnail(
                                file,
                                Size(iconSize, iconSize),
                                CancellationSignal()
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
                itemList.add(
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
        itemList.sortWith(compareByDescending<Item> { it.isDir }.thenBy { it.name })

        return itemList
    }

}
