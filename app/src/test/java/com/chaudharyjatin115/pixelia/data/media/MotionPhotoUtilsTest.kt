package com.chaudharyjatin115.pixelia.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionPhotoUtilsTest {

    @Test
    fun findsValidFtypBoxOffset() {
        val box = byteArrayOf(
            0, 0, 0, 16,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0, 0, 0, 0
        )

        assertEquals(100L, MotionPhotoUtils.findMp4FtypOffset(box, box.size, 100L, 116L, 4))
    }

    @Test
    fun ignoresFtypTextWithUnknownBrand() {
        val box = byteArrayOf(
            0, 0, 0, 16,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'n'.code.toByte(), 'o'.code.toByte(), 'p'.code.toByte(), 'e'.code.toByte(),
            0, 0, 0, 0
        )

        assertEquals(-1L, MotionPhotoUtils.findMp4FtypOffset(box, box.size, 100L, 116L, 4))
    }

    @Test
    fun rejectsFtypBoxLargerThanRemainingFile() {
        val box = byteArrayOf(
            0, 0, 0, 64,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0, 0, 0, 0
        )

        assertEquals(-1L, MotionPhotoUtils.findMp4FtypOffset(box, box.size, 100L, 116L, 4))
    }
}
