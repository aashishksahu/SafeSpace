package org.android.safespace.lib

import android.annotation.SuppressLint
import com.google.android.exoplayer2.util.MimeTypes
import java.text.SimpleDateFormat
import java.util.Date

class Utils {
    companion object {

        fun getFileType(fileName: String): String {
            val fileExtension = fileName.split(".").last()

            return when (fileExtension.lowercase()) {
                in Constants.IMAGE_EXTENSIONS -> {
                    Constants.IMAGE_TYPE
                }
                in Constants.AUDIO_EXTENSIONS -> {
                    Constants.AUDIO_TYPE
                }
                in Constants.DOCUMENT_EXTENSIONS -> {
                    Constants.DOCUMENT_TYPE
                }
                in Constants.VIDEO_EXTENSIONS -> {
                    Constants.VIDEO_TYPE
                }
                Constants.PDF -> {
                    Constants.PDF
                }
                Constants.TXT -> {
                    Constants.TXT
                }
                else -> Constants.OTHER_TYPE
            }

        }

        fun getSize(sizeInBytes: Long): String {

            val unit = arrayOf("Bytes", "KB", "MB", "GB", "TB")
            var unitIndex = 0
            var size: Double = sizeInBytes.toDouble()

            try {

                if (sizeInBytes in 0..1024) {
                    return sizeInBytes.toString() + " " + unit[unitIndex]
                } else {
                    while (size >= 1024) {
                        unitIndex += 1
                        size /= 1024.0
                    }
                }

            } catch (e: ArrayIndexOutOfBoundsException) {
                return "File size too big"
            }

            return "${String.format("%.1f", size)} ${unit[unitIndex]}"

        }

        fun getMimeType(fileName: String): String {

            val fileExtension = fileName.split(".").last()

            return when (fileExtension.lowercase()) {
                "mp4", "mov", "mp3" -> MimeTypes.APPLICATION_MP4
                "mkv" -> MimeTypes.VIDEO_MATROSKA
                "avi" -> MimeTypes.VIDEO_AVI
                else -> {
                    MimeTypes.APPLICATION_MP4
                }
            }

        }

        @SuppressLint("SimpleDateFormat")
        fun convertLongToTime(time: Long): String {
            val date = Date(time)
            val format = SimpleDateFormat("dd MMM, yyyy hh:mm a")
            return format.format(date)
        }
    }
}