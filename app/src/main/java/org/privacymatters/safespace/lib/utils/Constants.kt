package org.privacymatters.safespace.lib.utils

class Constants {
    companion object {

        const val TAG_ERROR = "org.privacymatters.safespace:E"
        const val ALREADY_EXISTS = "ALREADY_EXISTS"
        const val TIME_TO_UNLOCK_DURATION = "TIME_TO_UNLOCK_DURATION"
        const val TIME_TO_UNLOCK_START = "TIME_TO_UNLOCK_START"
        const val DEF_NUM_FLAG = -1L
        const val SHARED_PREF_FILE = "org.privacymatters.safespace_preferences"

        const val DOCUMENT_TYPE = "document"
        const val AUDIO_TYPE = "audio"
        const val VIDEO_TYPE = "video"
        const val IMAGE_TYPE = "image"

        const val OTHER_TYPE = "other"
        const val INTENT_KEY_PATH = "path"
        const val INTENT_KEY_INDEX = "index"

        const val CAMERA_MODE = "CAMERA_MODE"
        const val PHOTO = "PHOTO"
        const val VIDEO = "VIDEO"

        const val PDF = "pdf"
        const val TXT = "txt"
        const val JSON = "json"
        const val XML = "xml"
        const val ZIP = "zip"
        const val BIN = "BIN"

        const val ROOT = "root"
        const val APP_FIRST_RUN = "FIRST_RUN"
        const val USE_BIOMETRIC = "USE_BIOMETRIC"
        const val USE_BIOMETRIC_BCKP = "USE_BIOMETRIC_BCKP"

        const val FILE_EXIST = "FILE_EXIST"

        const val CAMERA_SELECTOR = "CAMERA_SELECTOR"
        const val DEFAULT_FRONT_CAMERA = "DEFAULT_FRONT_CAMERA"
        const val DEFAULT_BACK_CAMERA = "DEFAULT_BACK_CAMERA"

        const val NAME = "name"
        const val SIZE = "size"
        const val DATE = "date"
        const val ASC = "asc"
        const val DESC = "desc"
        const val FILE_SORT_BY = "file_sort_by"
        const val FILE_SORT_ORDER = "file_sort_order"

        const val HARD_PIN_SET = "HARD_PIN_SET"
        const val HARD_PIN = "HARD_PIN"

        val IMAGE_EXTENSIONS = arrayOf(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "webp",
            "tiff",
            "psd",
            "raw",
            "bmp",
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
