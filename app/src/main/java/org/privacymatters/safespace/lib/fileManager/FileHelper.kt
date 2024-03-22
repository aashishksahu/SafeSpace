package org.privacymatters.safespace.lib.fileManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Helper class for file operations
 */
object FileHelper {

    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer
    private const val INVALID_FILE_SIZE = -1000L

    /**
     * Copies a file from the input stream to the output stream with progress updates
     * @param inputStream The input stream to read from
     * @param outputStream The output stream to write to
     * @return A flow of progress updates
     */
    fun copyFileWithProgress(
        inputStream: InputStream,
        outputStream: FileOutputStream
    ): Flow<Int> = flow {
        var bytesCopied: Long = 0
        val fileSize = getInputLength(inputStream)

        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            bytesCopied += bytesRead.toLong()

            // Emit the progress percentage
            if (fileSize != INVALID_FILE_SIZE) {
                val progress = (bytesCopied * 100 / fileSize).toInt()
                emit(progress)
            }
        }

        outputStream.flush()
        inputStream.close()
        outputStream.close()

    }.flowOn(Dispatchers.IO)


    private fun getInputLength(inputStream: InputStream): Long {
        return try {
            when (inputStream) {
                is FilterInputStream -> {
                    val field = FilterInputStream::class.java.getDeclaredField("in")
                    field.isAccessible = true
                    val internal = field.get(inputStream) as InputStream
                    getInputLength(internal)
                }

                is ByteArrayInputStream -> {
                    val field = ByteArrayInputStream::class.java.getDeclaredField("buf")
                    field.isAccessible = true
                    val buffer = field.get(inputStream) as ByteArray
                    buffer.size.toLong()
                }

                is FileInputStream -> {
                    inputStream.channel.size()
                }

                else -> INVALID_FILE_SIZE
            }
        } catch (exception: NoSuchFieldException) {
            INVALID_FILE_SIZE
        } catch (exception: IllegalAccessException) {
            INVALID_FILE_SIZE
        } catch (exception: IOException) {
            INVALID_FILE_SIZE
        }
    }

}