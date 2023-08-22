package org.privacymatters.safespace.lib

import android.annotation.SuppressLint
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
                Constants.JSON -> {
                    Constants.JSON
                }
                Constants.XML -> {
                    Constants.XML
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

        @SuppressLint("SimpleDateFormat")
        fun convertLongToTime(time: Long): String {
            val date = Date(time)
            val format = SimpleDateFormat("dd MMM, yyyy hh:mm a")
            return format.format(date)
        }
    }
}