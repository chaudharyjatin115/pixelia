package com.chaudharyjatin115.pixelia.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(private val context: Context) {

    private val mediaComparator = compareByDescending<MediaItem> { it.dateTaken }
        .thenByDescending { it.dateModified }
        .thenByDescending { it.id }

    suspend fun queryFastInitialMedia(limit: Int = 300): List<MediaItem> =
        queryMedia(imageLimit = limit, videoLimit = limit / 3)

    suspend fun queryAllMedia(): List<MediaItem> =
        queryMedia(imageLimit = -1, videoLimit = -1)

    private suspend fun queryMedia(imageLimit: Int, videoLimit: Int): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false, maxItems = imageLimit)
        val videos = queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true, maxItems = videoLimit)
        (images + videos).sortedWith(mediaComparator)
    }

    private fun queryCollection(contentUri: Uri, isVideo: Boolean, maxItems: Int): List<MediaItem> {
        val initialCapacity = if (maxItems in 1..25000) maxItems else 4096
        val results = ArrayList<MediaItem>(initialCapacity)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.IS_FAVORITE,
            MediaStore.Video.VideoColumns.DURATION
        )

        try {
            val cursor = context.contentResolver.query(
                contentUri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateModCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val dateTakenCol = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val widthCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val bucketIdCol = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val dataCol = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                val relPathCol = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val favCol = c.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                val durationCol = c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

                var count = 0
                val hasLimit = maxItems > 0
                while (c.moveToNext() && (!hasLimit || count < maxItems)) {
                    count++
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(contentUri, id)
                    val name = c.getString(nameCol) ?: "Media_$id"
                    val mime = c.getString(mimeCol) ?: if (isVideo) "video/*" else "image/*"

                    val dateAddedSec = c.getLong(dateAddedCol)
                    val dateModSec = c.getLong(dateModCol)
                    val rawDateTaken = if (dateTakenCol >= 0 && !c.isNull(dateTakenCol)) c.getLong(dateTakenCol) else 0L

                    val dateTakenMs = when {
                        rawDateTaken > 0L -> rawDateTaken
                        dateModSec > 0L -> dateModSec * 1000L
                        else -> dateAddedSec * 1000L
                    }

                    val bucketId = if (bucketIdCol >= 0 && !c.isNull(bucketIdCol)) c.getString(bucketIdCol) ?: "" else ""
                    val bucketName = if (bucketNameCol >= 0 && !c.isNull(bucketNameCol)) {
                        c.getString(bucketNameCol) ?: "Photos"
                    } else "Photos"

                    val relPath = if (relPathCol >= 0 && !c.isNull(relPathCol)) {
                        c.getString(relPathCol) ?: ""
                    } else if (dataCol >= 0 && !c.isNull(dataCol)) {
                        c.getString(dataCol) ?: ""
                    } else ""

                    val isFavSystem = if (favCol >= 0 && !c.isNull(favCol)) c.getInt(favCol) == 1 else false
                    val duration = if (durationCol >= 0 && !c.isNull(durationCol)) c.getLong(durationCol) else 0L

                    results.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = name,
                            mimeType = mime,
                            isVideo = isVideo,
                            durationMs = duration,
                            dateTaken = dateTakenMs,
                            dateModified = dateModSec * 1000L,
                            sizeBytes = c.getLong(sizeCol),
                            width = c.getInt(widthCol),
                            height = c.getInt(heightCol),
                            bucketId = bucketId,
                            bucketName = bucketName,
                            relativePath = relPath,
                            isFavorite = isFavSystem,
                            isTrashed = false,
                            trashedTime = 0L
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission restricted or revoked by system
            Log.w("MediaStoreScanner", "Storage permission denied while querying $contentUri", e)
        } catch (e: IllegalArgumentException) {
            // Projection column missing on custom ROM/provider
            Log.e("MediaStoreScanner", "Invalid column projection for $contentUri", e)
        } catch (e: IllegalStateException) {
            // ContentResolver or provider state exception
            Log.e("MediaStoreScanner", "ContentResolver error while querying $contentUri", e)
        }

        return results
    }
}
