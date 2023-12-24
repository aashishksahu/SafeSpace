package org.privacymatters.safespace.lib

class Constants {
    companion object {

        const val SHARED_PREF_FILE = "org.privacymatters.safespace_preferences"

        const val DOCUMENT_TYPE = "document"
        const val AUDIO_TYPE = "audio"
        const val VIDEO_TYPE = "video"
        const val IMAGE_TYPE = "image"
        const val OTHER_TYPE = "other"
        const val INTENT_KEY_PATH = "path"

        const val CAMERA_MODE = "CAMERA_MODE"
        const val PHOTO = "PHOTO"
        const val VIDEO = "VIDEO"

        const val PDF = "pdf"
        const val TXT = "txt"
        const val JSON = "json"
        const val XML = "xml"
        const val ROOT = "root"
        const val APP_FIRST_RUN = "FIRST_RUN"

        const val FILE_EXIST = "FILE_EXIST"

        const val CAMERA_SELECTOR = "CAMERA_SELECTOR"
        const val DEFAULT_FRONT_CAMERA = "DEFAULT_FRONT_CAMERA"
        const val DEFAULT_BACK_CAMERA = "DEFAULT_BACK_CAMERA"

        const val NAME = "name"
        const val SIZE = "size"
        const val DATE = "date"
        const val ASC = "asc"
        const val DESC = "desc"
        const val FOLDER_SORT = "folder_sort"
        const val SORT_TYPE = "sort_type"
        const val SORT_ORDER = "sort_order"

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
