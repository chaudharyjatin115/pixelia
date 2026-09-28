package com.chaudharyjatin115.pixelia.domain.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class MediaFolderTest {

    @Test
    fun testMediaFolderProperties() {
        val mockUri = mock(Uri::class.java)
        val folder = MediaFolder(
            bucketId = "Camera",
            bucketName = "Camera",
            coverUri = mockUri,
            mediaCount = 15,
            latestDateTaken = 1000L,
            totalSizeBytes = 10485760L
        )

        assertEquals("Camera", folder.bucketId)
        assertEquals("Camera", folder.bucketName)
        assertEquals(15, folder.mediaCount)
        assertEquals("10.0 MB", folder.formattedSize)
    }
}
