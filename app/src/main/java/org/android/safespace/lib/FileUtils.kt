package org.android.safespace.lib

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.*


class FileUtils(private val context: Context) {

    private var filesList: ArrayList<FileItem> = ArrayList()

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

        } catch (e: Exception) {
            println(e.message)
            return -1
        } finally {
            parcelFileDescriptor?.close()
        }

        return 1
    }

    fun renameFile(file: File){

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

    fun getContents(context: Context, internalPath: String): List<FileItem> {

        val dirPath = File(context.filesDir.absolutePath + File.separator + internalPath)

        val contents = dirPath.listFiles()

        filesList.clear()

        for (item in contents!!) {
            filesList.add(FileItem(item.name, item.length(), item.isDirectory))
        }

        // sort -> folders first -> ascending by name
        filesList.sortWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name })

        return filesList

    }

}