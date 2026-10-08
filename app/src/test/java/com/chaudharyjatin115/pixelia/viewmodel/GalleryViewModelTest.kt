package com.chaudharyjatin115.pixelia.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.chaudharyjatin115.pixelia.domain.model.MediaFolder
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = mock(Application::class.java)
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val prefs = mock(SharedPreferences::class.java)

        `when`(application.applicationContext).thenReturn(context)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)

        viewModel = GalleryViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectDestination_updatesDestinationAndResetsFolderAndSelection() {
        val dummyUri = mock(Uri::class.java)
        viewModel.openFolder(MediaFolder(bucketId = "100", bucketName = "Camera", coverUri = dummyUri, mediaCount = 5, latestDateTaken = 1000L))
        viewModel.toggleSelectMedia(1L)

        viewModel.selectDestination(GalleryDestination.FOLDERS)

        assertEquals(GalleryDestination.FOLDERS, viewModel.selectedDestination.value)
        assertNull(viewModel.activeFolder.value)
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedMediaIds.value.isEmpty())
    }

    @Test
    fun openFolder_setsActiveFolderAndResetsSelection() {
        val dummyUri = mock(Uri::class.java)
        val folder = MediaFolder(bucketId = "200", bucketName = "Downloads", coverUri = dummyUri, mediaCount = 10, latestDateTaken = 2000L)
        viewModel.toggleSelectMedia(2L)

        viewModel.openFolder(folder)

        assertEquals(folder, viewModel.activeFolder.value)
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedMediaIds.value.isEmpty())
    }

    @Test
    fun closeFolder_clearsActiveFolder() {
        val dummyUri = mock(Uri::class.java)
        val folder = MediaFolder(bucketId = "200", bucketName = "Downloads", coverUri = dummyUri, mediaCount = 10, latestDateTaken = 2000L)
        viewModel.openFolder(folder)

        viewModel.closeFolder()

        assertNull(viewModel.activeFolder.value)
    }

    @Test
    fun toggleSelectMedia_togglesItemAndUpdatesSelectionMode() {
        viewModel.toggleSelectMedia(10L)

        assertTrue(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedMediaIds.value.contains(10L))

        viewModel.toggleSelectMedia(10L)

        assertFalse(viewModel.isSelectionMode.value)
        assertFalse(viewModel.selectedMediaIds.value.contains(10L))
    }

    @Test
    fun selectAll_addsAllItemIdsToSelection() {
        val dummyUri = mock(Uri::class.java)
        val items = listOf(
            MediaItem(id = 1L, uri = dummyUri, name = "img1.jpg", mimeType = "image/jpeg", isVideo = false),
            MediaItem(id = 2L, uri = dummyUri, name = "img2.jpg", mimeType = "image/jpeg", isVideo = false)
        )

        viewModel.selectAll(items)

        assertTrue(viewModel.isSelectionMode.value)
        assertEquals(setOf(1L, 2L), viewModel.selectedMediaIds.value)
    }

    @Test
    fun clearSelection_clearsAllSelectedIdsAndExitsSelectionMode() {
        viewModel.toggleSelectMedia(5L)
        viewModel.toggleSelectMedia(6L)

        viewModel.clearSelection()

        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedMediaIds.value.isEmpty())
    }

    @Test
    fun setSelectedMedia_updatesSelectedIdsAndSelectionMode() {
        viewModel.setSelectedMedia(setOf(100L, 101L))

        assertTrue(viewModel.isSelectionMode.value)
        assertEquals(setOf(100L, 101L), viewModel.selectedMediaIds.value)

        viewModel.setSelectedMedia(emptySet())

        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedMediaIds.value.isEmpty())
    }

    @Test
    fun openViewer_setsViewerMediaListAndIndex() {
        val dummyUri = mock(Uri::class.java)
        val item1 = MediaItem(id = 1L, uri = dummyUri, name = "photo1.jpg", mimeType = "image/jpeg", isVideo = false)
        val item2 = MediaItem(id = 2L, uri = dummyUri, name = "photo2.jpg", mimeType = "image/jpeg", isVideo = false)
        val list = listOf(item1, item2)

        viewModel.openViewer(item2, list)

        assertEquals(list, viewModel.viewerMediaList.value)
        assertEquals(1, viewModel.viewerInitialIndex.value)
    }

    @Test
    fun closeViewer_clearsViewerMediaList() {
        val dummyUri = mock(Uri::class.java)
        val item = MediaItem(id = 1L, uri = dummyUri, name = "photo1.jpg", mimeType = "image/jpeg", isVideo = false)
        viewModel.openViewer(item, listOf(item))

        viewModel.closeViewer()

        assertTrue(viewModel.viewerMediaList.value.isEmpty())
    }
}
