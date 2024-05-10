package org.privacymatters.safespace.lib.fileManager

import android.annotation.SuppressLint
import org.privacymatters.safespace.lib.utils.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("unused")
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

                Constants.ZIP -> {
                    Constants.ZIP
                }

                else -> Constants.OTHER_TYPE
            }

        }

        @SuppressLint("DefaultLocale")
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

        @SuppressLint("SimpleDateFormat")
        fun convertLongToDate(time: Long): String {
            val date = Date(time)
            val format = SimpleDateFormat("dd MMM, yyyy")
            return format.format(date)
        }

        fun getFileNameAndExtension(name: String): Pair<String, String> {
            var nameOnly = name
            var ext = "BIN"

            val lastDelimiterIndex = name.lastIndexOf('.')

            if (lastDelimiterIndex != -1) {
                nameOnly = name.substring(0, lastDelimiterIndex)
                ext = name.substring(lastDelimiterIndex + 1)

            }

            return Pair(nameOnly, ext)

        }

        fun getPathAndFileName(name: String): Pair<String, String> {
            var path = name
            var file = ""

            val lastDelimiterIndex = name.lastIndexOf(File.separator)

            if (lastDelimiterIndex != -1) {
                path = name.substring(0, lastDelimiterIndex)
                file = name.substring(lastDelimiterIndex + 1)

            }

            return Pair(path, file)

        }
    }
}