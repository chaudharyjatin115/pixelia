package com.chaudharyjatin115.pixelia.data.repository

import android.content.Context
import android.content.SharedPreferences

class LocalMediaStateStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gallery_local_state", Context.MODE_PRIVATE)

    fun getFavoriteIds(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    fun getUnfavoriteIds(): Set<String> =
        prefs.getStringSet(KEY_UNFAVORITES, emptySet()) ?: emptySet()

    fun setFavorite(mediaId: String, isFavorite: Boolean) {
        val favs = HashSet(getFavoriteIds())
        val unfavs = HashSet(getUnfavoriteIds())
        if (isFavorite) {
            favs.add(mediaId)
            unfavs.remove(mediaId)
        } else {
            favs.remove(mediaId)
            unfavs.add(mediaId)
        }
        prefs.edit()
            .putStringSet(KEY_FAVORITES, favs)
            .putStringSet(KEY_UNFAVORITES, unfavs)
            .apply()
    }

    fun getTrashedIds(): Set<String> =
        prefs.getStringSet(KEY_TRASHED, emptySet()) ?: emptySet()

    fun getTrashTimestamp(mediaId: String): Long =
        prefs.getLong(KEY_TRASH_TIME_PREFIX + mediaId, 0L)

    fun moveToTrash(mediaId: String) {
        val current = HashSet(getTrashedIds())
        current.add(mediaId)
        prefs.edit()
            .putStringSet(KEY_TRASHED, current)
            .putLong(KEY_TRASH_TIME_PREFIX + mediaId, System.currentTimeMillis())
            .apply()
    }

    fun batchMoveToTrash(mediaIds: Collection<String>) {
        if (mediaIds.isEmpty()) return
        val current = HashSet(getTrashedIds())
        current.addAll(mediaIds)
        val editor = prefs.edit().putStringSet(KEY_TRASHED, current)
        val now = System.currentTimeMillis()
        for (id in mediaIds) {
            editor.putLong(KEY_TRASH_TIME_PREFIX + id, now)
        }
        editor.apply()
    }

    fun restoreFromTrash(mediaId: String) {
        val current = HashSet(getTrashedIds())
        current.remove(mediaId)
        prefs.edit()
            .putStringSet(KEY_TRASHED, current)
            .remove(KEY_TRASH_TIME_PREFIX + mediaId)
            .apply()
    }

    fun batchRestoreFromTrash(mediaIds: Collection<String>) {
        if (mediaIds.isEmpty()) return
        val current = HashSet(getTrashedIds())
        current.removeAll(mediaIds.toSet())
        val editor = prefs.edit().putStringSet(KEY_TRASHED, current)
        for (id in mediaIds) {
            editor.remove(KEY_TRASH_TIME_PREFIX + id)
        }
        editor.apply()
    }

    fun clearTrash() {
        val trashed = getTrashedIds()
        val editor = prefs.edit()
        for (id in trashed) {
            editor.remove(KEY_TRASH_TIME_PREFIX + id)
        }
        editor.remove(KEY_TRASHED)
        editor.apply()
    }

    fun removeMediaIds(mediaIds: Collection<String>) {
        if (mediaIds.isEmpty()) return
        val currentTrash = HashSet(getTrashedIds())
        currentTrash.removeAll(mediaIds.toSet())
        val currentFav = HashSet(getFavoriteIds())
        currentFav.removeAll(mediaIds.toSet())
        val currentUnfav = HashSet(getUnfavoriteIds())
        currentUnfav.removeAll(mediaIds.toSet())

        val editor = prefs.edit()
            .putStringSet(KEY_TRASHED, currentTrash)
            .putStringSet(KEY_FAVORITES, currentFav)
            .putStringSet(KEY_UNFAVORITES, currentUnfav)
        for (id in mediaIds) {
            editor.remove(KEY_TRASH_TIME_PREFIX + id)
        }
        editor.apply()
    }

    companion object {
        private const val KEY_FAVORITES = "pref_favorites"
        private const val KEY_UNFAVORITES = "pref_unfavorites"
        private const val KEY_TRASHED = "pref_trashed"
        private const val KEY_TRASH_TIME_PREFIX = "trash_time_"
    }
}
