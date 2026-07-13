package com.chaudharyjatin115.pixelia.domain.model

import android.net.Uri
import java.util.Locale

data class MediaFolder(
    val bucketId: String,
    val bucketName: String,
    val coverUri: Uri?,
    val mediaCount: Int,
    val latestDateTaken: Long,
    val latestDateModified: Long = 0L,
    val totalSizeBytes: Long = 0L
) {
    val formattedSize: String
        get() {
            if (totalSizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var size = totalSizeBytes.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.lastIndex) {
                size /= 1024
                unitIndex++
            }
            return if (unitIndex == 0) {
                String.format(Locale.US, "%d %s", totalSizeBytes, units[unitIndex])
            } else if (size >= 100) {
                String.format(Locale.US, "%.0f %s", size, units[unitIndex])
            } else {
                String.format(Locale.US, "%.1f %s", size, units[unitIndex])
            }
        }
}
