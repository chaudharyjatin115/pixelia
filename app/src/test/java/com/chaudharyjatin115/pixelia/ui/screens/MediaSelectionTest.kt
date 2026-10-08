package com.chaudharyjatin115.pixelia.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSelectionTest {
    private val mediaIds = listOf(10L, 20L, 30L, 40L, 50L)

    @Test
    fun mediaIdRange_selectsInclusiveRangeInBothDirections() {
        assertEquals(setOf(20L, 30L, 40L), mediaIdRange(mediaIds, 20L, 40L))
        assertEquals(setOf(20L, 30L, 40L), mediaIdRange(mediaIds, 40L, 20L))
    }

    @Test
    fun mediaIdRange_returnsEmptyWhenAnEndpointIsMissing() {
        assertEquals(emptySet<Long>(), mediaIdRange(mediaIds, 20L, 60L))
    }
}
