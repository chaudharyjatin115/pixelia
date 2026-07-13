package com.chaudharyjatin115.pixelia.domain.model

data class DateGroupedMedia(
    val header: String,
    val dateKey: String,
    val items: List<MediaItem>
)
