package com.chaudharyjatin115.pixelia.data.repository

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LocalMediaStateStoreTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var store: LocalMediaStateStore

    private val inMemoryStringSets = mutableMapOf<String, Set<String>>()
    private val inMemoryLongs = mutableMapOf<String, Long>()

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)

        `when`(editor.putStringSet(anyString(), anySet())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<Set<String>>(1)
            inMemoryStringSets[key] = value
            editor
        }

        `when`(editor.putLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<Long>(1)
            inMemoryLongs[key] = value
            editor
        }

        `when`(editor.remove(anyString())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            inMemoryStringSets.remove(key)
            inMemoryLongs.remove(key)
            editor
        }

        `when`(prefs.getStringSet(anyString(), anySet())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<Set<String>>(1)
            inMemoryStringSets[key] ?: defaultVal
        }

        `when`(prefs.getLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<Long>(1)
            inMemoryLongs[key] ?: defaultVal
        }

        store = LocalMediaStateStore(context)
    }

    @Test
    fun setFavorite_addsToFavoritesAndRemovesFromUnfavorites() {
        store.setFavorite("101", true)

        assertTrue(store.getFavoriteIds().contains("101"))
        assertFalse(store.getUnfavoriteIds().contains("101"))

        store.setFavorite("101", false)

        assertFalse(store.getFavoriteIds().contains("101"))
        assertTrue(store.getUnfavoriteIds().contains("101"))
    }

    @Test
    fun batchMoveToTrash_addsMultipleIdsAndSetsTimestamps() {
        store.batchMoveToTrash(listOf("1", "2"))

        val trashed = store.getTrashedIds()
        assertTrue(trashed.contains("1"))
        assertTrue(trashed.contains("2"))
        assertTrue(store.getTrashTimestamp("1") > 0)
        assertTrue(store.getTrashTimestamp("2") > 0)
    }

    @Test
    fun batchRestoreFromTrash_removesIdsAndTimestamps() {
        store.batchMoveToTrash(listOf("10", "20"))
        store.batchRestoreFromTrash(listOf("10"))

        val trashed = store.getTrashedIds()
        assertFalse(trashed.contains("10"))
        assertTrue(trashed.contains("20"))
        assertEquals(0L, store.getTrashTimestamp("10"))
    }

    @Test
    fun clearTrash_removesAllTrashedItemsAndTimestamps() {
        store.batchMoveToTrash(listOf("100", "200"))
        store.clearTrash()

        assertTrue(store.getTrashedIds().isEmpty())
        assertEquals(0L, store.getTrashTimestamp("100"))
        assertEquals(0L, store.getTrashTimestamp("200"))
    }
}
