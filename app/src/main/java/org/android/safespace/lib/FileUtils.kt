package org.android.safespace.lib

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.*


class FileUtils(private val context: Context) {

    fun importFile(uri: Uri, internalPath: String): Int {

        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")

        // create directory if not exists
        createDir(internalPath, "")

        try {

            // source File from URI
            val sourceFile = File(uri.path!!)
            // byte array of source file
            val sourceFileByteArray = FileInputStream(parcelFileDescriptor?.fileDescriptor)

            // files directory in internal storage + the current subdirectory
            val absoluteDestination = context.filesDir.absolutePath + internalPath

            // files directory in internal storage + the current subdirectory + file name
            val absoluteDestinationFilePath = absoluteDestination + File.separator + sourceFile.name

            // output stream for destination
            val outputStreamDestinationFile = FileOutputStream(File(absoluteDestinationFilePath))

            // write 1 MiB of data from outputStreamDestinationFile to internal storage
            val buf = ByteArray(1024)
            var len: Int
            while (sourceFileByteArray.read(buf).also { len = it } > 0) {
                outputStreamDestinationFile.write(buf, 0, len)
            }

            // rename file
            val returnCursor = context.contentResolver.query(uri, null, null, null, null)!!

            val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            val name: String = returnCursor.getString(nameIndex)
            returnCursor.close()

            val destinationFileOldName = File(absoluteDestinationFilePath)
            val destinationFileNewName = File(absoluteDestination + File.separator + name)

            destinationFileOldName.renameTo(destinationFileNewName)

            sourceFileByteArray.close()
            outputStreamDestinationFile.close()

        } catch (e: FileSystemException) {
            println(e.message)
            return -1
        } catch (e: FileNotFoundException) {
            println(e.message)
            return -2
        } catch (e: IOException) {
            println(e.message)
            return -3
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