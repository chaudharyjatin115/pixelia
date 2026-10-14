package com.chaudharyjatin115.pixelia.data.media

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer

object MotionPhotoUtils {

    private const val TAG = "MotionPhotoUtils"
    private const val SEARCH_CHUNK_SIZE = 64 * 1024

    suspend fun isRealMotionPhoto(context: Context, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        findMotionVideoOffset(context, imageUri) > 0L
    }

    suspend fun extractMotionVideo(context: Context, imageUri: Uri): File? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val offset = findMotionVideoOffset(context, imageUri)
            if (offset <= 0L) return@withContext null

            cleanOldMotionTempFiles(context)

            tempFile = File.createTempFile("motion_video_", ".mp4", context.cacheDir)
            tempFile.deleteOnExit()

            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw FileNotFoundException("Unable to open motion photo URI: $imageUri")
            inputStream.use {
                var skipped = 0L
                while (skipped < offset) {
                    currentCoroutineContext().ensureActive()
                    val skippedNow = it.skip(offset - skipped)
                    if (skippedNow > 0L) {
                        skipped += skippedNow
                    } else if (it.read() == -1) {
                        break
                    } else {
                        skipped++
                    }
                }
                if (skipped < offset) {
                    tempFile.delete()
                    return@withContext null
                }

                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = it.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                }
            }

            if (tempFile.length() > 0) {
                tempFile
            } else {
                tempFile.delete()
                null
            }
        } catch (e: CancellationException) {
            tempFile?.delete()
            throw e
        } catch (e: SecurityException) {
            Log.w(TAG, "Storage permission restricted while extracting motion photo: $imageUri", e)
            tempFile?.delete()
            null
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "Motion photo URI file not found: $imageUri", e)
            tempFile?.delete()
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting motion video from: $imageUri", e)
            tempFile?.delete()
            null
        }
    }

    private suspend fun findMotionVideoOffset(context: Context, imageUri: Uri): Long {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(imageUri, "r") ?: return -1L
            pfd.use { descriptor ->
                val fileSize = descriptor.statSize
                if (fileSize < 8192) return -1L

                val fileChannel = FileInputStream(descriptor.fileDescriptor).channel
                var windowEnd = fileSize
                while (windowEnd > 0L) {
                    currentCoroutineContext().ensureActive()
                    val windowStart = (windowEnd - SEARCH_CHUNK_SIZE).coerceAtLeast(0L)
                    val buffer = ByteBuffer.allocate((windowEnd - windowStart).toInt())
                    fileChannel.position(windowStart)
                    while (buffer.hasRemaining()) {
                        currentCoroutineContext().ensureActive()
                        if (fileChannel.read(buffer) <= 0) break
                    }
                    val bytesRead = buffer.position()
                    val bytes = buffer.array()
                    for (i in bytesRead - 12 downTo 4) {
                        val offset = findMp4FtypOffset(bytes, bytesRead, windowStart, fileSize, i)
                        if (offset >= 0L) return@use offset
                    }
                    if (windowStart == 0L) break
                    windowEnd = windowStart + 12L
                }
                -1L
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException opening file descriptor for: $imageUri", e)
            -1L
        } catch (e: Exception) {
            Log.w(TAG, "Error finding motion video offset for: $imageUri", e)
            -1L
        }
    }

    internal fun findMp4FtypOffset(
        bytes: ByteArray,
        bytesRead: Int,
        windowStart: Long,
        fileSize: Long,
        typeIndex: Int
    ): Long {
        if (bytesRead > bytes.size || typeIndex < 4 || typeIndex + 12 > bytesRead) return -1L
        if (bytes[typeIndex] != 'f'.code.toByte() ||
            bytes[typeIndex + 1] != 't'.code.toByte() ||
            bytes[typeIndex + 2] != 'y'.code.toByte() ||
            bytes[typeIndex + 3] != 'p'.code.toByte()
        ) {
            return -1L
        }

        val boxStart = windowStart + typeIndex - 4L
        val boxSize = (((bytes[typeIndex - 4].toLong() and 0xFF) shl 24) or
            ((bytes[typeIndex - 3].toLong() and 0xFF) shl 16) or
            ((bytes[typeIndex - 2].toLong() and 0xFF) shl 8) or
            (bytes[typeIndex - 1].toLong() and 0xFF))
        if (boxSize < 16L || boxSize > fileSize - boxStart) return -1L

        val brand = String(bytes, typeIndex + 4, 4, Charsets.US_ASCII)
        if (brand !in setOf("isom", "iso2", "mp41", "mp42", "avc1", "M4V ", "3gp4", "3gp5")) {
            return -1L
        }
        return boxStart
    }

    private fun cleanOldMotionTempFiles(context: Context) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles { _, name -> name.startsWith("motion_video_") && name.endsWith(".mp4") }
                ?.forEach { file ->
                    if (System.currentTimeMillis() - file.lastModified() > 10 * 60 * 1000L) {
                        file.delete()
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning old motion temp files", e)
        }
    }
}
