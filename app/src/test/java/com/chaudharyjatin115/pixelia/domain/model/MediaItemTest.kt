package com.chaudharyjatin115.pixelia.domain.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class MediaItemTest {

    @Test
    fun testIsVideo() {
        val mockUri = mock(Uri::class.java)
        val imageItem = MediaItem(
            id = 1L,
            uri = mockUri,
            name = "test.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            sizeBytes = 1024L
        )

        val videoItem = MediaItem(
            id = 2L,
            uri = mockUri,
            name = "video.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            durationMs = 5000L,
            sizeBytes = 2048L
        )

        assertFalse(imageItem.isVideo)
        assertTrue(videoItem.isVideo)
    }

    @Test
    fun testFormattedSize() {
        val mockUri = mock(Uri::class.java)
        val item = MediaItem(
            id = 1L,
            uri = mockUri,
            name = "sample.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            sizeBytes = 10485760L // 10 MB
        )

        assertEquals("10.0 MB", item.formattedSize)
    }
}
