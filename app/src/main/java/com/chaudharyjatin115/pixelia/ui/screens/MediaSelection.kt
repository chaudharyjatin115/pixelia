package com.chaudharyjatin115.pixelia.ui.screens

internal fun mediaIdRange(ids: List<Long>, anchorId: Long, currentId: Long): Set<Long> {
    val anchorIndex = ids.indexOf(anchorId)
    val currentIndex = ids.indexOf(currentId)
    if (anchorIndex < 0 || currentIndex < 0) return emptySet()

    val firstIndex = minOf(anchorIndex, currentIndex)
    val lastIndex = maxOf(anchorIndex, currentIndex)
    return ids.subList(firstIndex, lastIndex + 1).toSet()
}
