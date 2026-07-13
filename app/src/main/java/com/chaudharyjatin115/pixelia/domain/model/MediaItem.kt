package com.chaudharyjatin115.pixelia.domain.model

import android.net.Uri
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val dateTaken: Long = 0L,
    val dateModified: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val bucketId: String = "",
    val bucketName: String = "",
    val relativePath: String = "",
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedTime: Long = 0L
) {
    val formattedDuration: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
            return String.format(Locale.US, "%d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = sizeBytes.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.lastIndex) {
                size /= 1024
                unitIndex++
            }
            return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
        }

    val resolutionText: String
        get() = if (width > 0 && height > 0) "$width × $height" else ""
}
