package org.android.safespace.lib

class Constants {
    companion object {

        const val DOCUMENT_TYPE = "document"
        const val AUDIO_TYPE = "audio"
        const val VIDEO_TYPE = "video"
        const val IMAGE_TYPE = "image"
        const val OTHER_TYPE = "other"
        const val INTENT_KEY_PATH = "path"

        const val PDF = "pdf"
        const val TXT = "txt"
        const val JSON = "json"
        const val XML = "xml"
        const val ROOT = "root"
        const val APP_FIRST_RUN = "FIRST_RUN"

        const val FILE_EXIST = "FILE_EXIST"
        const val PIN_SETUP_COMPLETE = "PIN_SETUP_COMPLETE"


        val IMAGE_EXTENSIONS = arrayOf(
            "jpg",
            "png",
            "gif",
            "webp",
            "tiff",
            "psd",
            "raw",
            "bmp",
            "svg",
            "heif"
        )

        val AUDIO_EXTENSIONS = arrayOf(
            "aif",
            "cd",
            "midi",
            "mp3",
            "mp2",
            "m4b",
            "mpeg",
            "ogg",
            "wav",
            "wma"
        )

//       commented formats are not internally supported
        val DOCUMENT_EXTENSIONS = arrayOf(
            "csv",
            "dat",
//            "db",
//            "log",
//            "mdb",
//            "sav",
//            "sql",
//            "tar",
//            "ods",
//            "xlsx",
//            "xls",
//            "xlsm",
//            "xlsb",
//            "xml",
//            "doc",
//            "odt",
//            "rtf",
//            "tex",
//            "wpd"
        )

        val VIDEO_EXTENSIONS = arrayOf(
            "3g2",
            "3gp",
            "avi",
            "flv",
            "h264",
            "m4v",
            "mkv",
            "mov",
            "mp4",
            "mpg",
            "mpeg",
            "rm",
            "swf",
            "vob",
            "webm",
            "wmv"
        )

    }
}
