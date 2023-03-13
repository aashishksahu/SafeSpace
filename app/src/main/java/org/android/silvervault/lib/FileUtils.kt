package org.android.silvervault.lib

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class FileUtils {

    fun importFile(context: Context, uri: Uri) {

        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")

        try {

            val sourceFile = File(uri.path!!)

            val sourceFileByteArray = FileInputStream(parcelFileDescriptor?.fileDescriptor)

            context.openFileOutput(sourceFile.name, Context.MODE_PRIVATE).use {
                it.write(sourceFileByteArray.readBytes())
            }

            createDir(context, "whatever")

        } catch (e: FileSystemException) {
            println(e.message)
        } catch (e: IOException) {
            println(e.message)
        } finally {
            parcelFileDescriptor?.close()
        }
    }

//    Todo: create function for nested folder creation

    fun createDir(context: Context, dirName: String) {
        val filesDir: File = context.getDir("files", Context.MODE_PRIVATE)

        val newDir = File(filesDir, dirName)

        if (!newDir.exists()) {
            newDir.mkdirs()
        }
    }

}