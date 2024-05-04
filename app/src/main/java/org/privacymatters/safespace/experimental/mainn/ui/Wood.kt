package org.privacymatters.safespace.experimental.mainn.ui

import android.os.Environment
import org.privacymatters.safespace.lib.utils.Constants


fun getLogcat() {

    val filePath = Environment.getExternalStorageDirectory().toString() + "/safe-space-logcat.txt"

    Runtime.getRuntime()
        .exec(
            arrayOf(
                "logcat",
                "-f",
                filePath,
                Constants.TAG_ERROR,
                "*:E"
            ).joinToString(" ")
        )


}
