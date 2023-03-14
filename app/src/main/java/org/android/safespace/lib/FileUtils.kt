package org.android.safespace.lib

import android.content.Context
import android.net.Uri
import java.io.*

class FileUtils(private val context: Context) {

    fun importFile(uri: Uri, internalPath: String): Int {

        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")

        createDir(internalPath, "")

        try {

            val sourceFile = File(uri.path!!)

            val absolutePathWithFile =
                context.filesDir.absolutePath + internalPath + File.separator + sourceFile.name

            val sourceFileByteArray = FileInputStream(parcelFileDescriptor?.fileDescriptor)

            val out: OutputStream = FileOutputStream(File(absolutePathWithFile))

            // Todo: Fix - Incorrect file name
            val buf = ByteArray(1024)
            var len: Int
            while (sourceFileByteArray.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }

            sourceFileByteArray.close()
            out.close()

        } catch (e: FileSystemException) {
            return -1
        } catch (e: IOException) {
            return -2
        } finally {
            parcelFileDescriptor?.close()
        }

        return 1
    }

    fun createDir(internalPath: String, newDirName: String): Int {

        try {
            val dirPath = if (newDirName == "") {
                context.filesDir.absolutePath + internalPath
            } else {
                context.filesDir.absolutePath + internalPath + File.separator + newDirName
            }

            val newDir = File(dirPath)

            if (!newDir.exists()) {
                newDir.mkdirs()
            }
        } catch (e: FileSystemException) {
            return 0
        }

        return 1

    }

}