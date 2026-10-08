package com.chaudharyjatin115.pixelia.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaudharyjatin115.pixelia.data.repository.MediaRepository
import com.chaudharyjatin115.pixelia.domain.model.DateGroupedMedia
import com.chaudharyjatin115.pixelia.domain.model.MediaFolder
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import com.chaudharyjatin115.pixelia.ui.navigation.FolderAction
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(
        context = application.applicationContext,
        externalScope = viewModelScope
    )

    private val _selectedDestination = MutableStateFlow(GalleryDestination.PHOTOS)
    val selectedDestination: StateFlow<GalleryDestination> = _selectedDestination.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _isPartialAccess = MutableStateFlow(false)
    val isPartialAccess: StateFlow<Boolean> = _isPartialAccess.asStateFlow()

    private val _activeFolder = MutableStateFlow<MediaFolder?>(null)
    val activeFolder: StateFlow<MediaFolder?> = _activeFolder.asStateFlow()

    // Multi-selection state
    private val _selectedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMediaIds: StateFlow<Set<Long>> = _selectedMediaIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _activeFolderAction = MutableStateFlow<FolderAction?>(FolderAction.SHARE)
    val activeFolderAction: StateFlow<FolderAction?> = _activeFolderAction.asStateFlow()

    // Scoped storage permanent delete request
    private val _pendingDeleteRequest = MutableStateFlow<IntentSenderRequest?>(null)
    val pendingDeleteRequest: StateFlow<IntentSenderRequest?> = _pendingDeleteRequest.asStateFlow()
    private var pendingConsentItems: List<MediaItem> = emptyList()

    // Photo viewer state
    private val _viewerMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val viewerMediaList: StateFlow<List<MediaItem>> = _viewerMediaList.asStateFlow()

    private val _viewerInitialIndex = MutableStateFlow(0)
    val viewerInitialIndex: StateFlow<Int> = _viewerInitialIndex.asStateFlow()

    val isViewerOpen: StateFlow<Boolean> = _viewerMediaList
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Data streams from repository
    val allMedia: StateFlow<List<MediaItem>> = repository.allMedia
    val groupedMedia: StateFlow<List<DateGroupedMedia>> = repository.groupedMedia
    val folders: StateFlow<List<MediaFolder>> = repository.folders
    val favorites: StateFlow<List<MediaItem>> = repository.favorites
    val binMedia: StateFlow<List<MediaItem>> = repository.binMedia
    val isLoading: StateFlow<Boolean> = repository.isLoading

    fun updatePermissionState(hasFull: Boolean, hasPartial: Boolean) {
        _hasStoragePermission.value = hasFull || hasPartial
        _isPartialAccess.value = hasPartial && !hasFull
        if (hasFull || hasPartial) {
            repository.refresh()
        }
    }

    fun selectDestination(destination: GalleryDestination) {
        _selectedDestination.value = destination
        _activeFolder.value = null
        clearSelection()
    }

    fun openFolder(folder: MediaFolder) {
        _activeFolder.value = folder
        _activeFolderAction.value = FolderAction.SHARE
        clearSelection()
    }

    fun closeFolder() {
        _activeFolder.value = null
        clearSelection()
    }

    fun toggleSelectMedia(id: Long) {
        val current = _selectedMediaIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedMediaIds.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun startSelectionMode(initialId: Long? = null) {
        _isSelectionMode.value = true
        if (initialId != null) {
            _selectedMediaIds.value = setOf(initialId)
        }
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedMediaIds.value = items.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun setSelectedMedia(ids: Set<Long>) {
        _selectedMediaIds.value = ids
        _isSelectionMode.value = ids.isNotEmpty()
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun setActiveFolderAction(action: FolderAction?) {
        _activeFolderAction.value = action
    }

    fun getItemsForActiveFolder(): List<MediaItem> {
        val folder = _activeFolder.value ?: return emptyList()
        return repository.getItemsInFolder(folder.bucketId)
    }

    fun getItemsForFolder(folder: MediaFolder): List<MediaItem> {
        return repository.getItemsInFolder(folder.bucketId)
    }

    fun openViewer(mediaItem: MediaItem, inList: List<MediaItem>) {
        if (_isSelectionMode.value || _selectedMediaIds.value.isNotEmpty()) {
            toggleSelectMedia(mediaItem.id)
            return
        }
        val index = inList.indexOfFirst { it.id == mediaItem.id }.coerceAtLeast(0)
        _viewerInitialIndex.value = index
        _viewerMediaList.value = inList
    }

    fun closeViewer() {
        _viewerMediaList.value = emptyList()
        _viewerInitialIndex.value = 0
    }

    fun toggleFavorite(item: MediaItem) {
        val newFav = !item.isFavorite
        repository.toggleFavorite(item)
        _viewerMediaList.value = _viewerMediaList.value.map {
            if (it.id == item.id) it.copy(isFavorite = newFav) else it
        }
    }

    fun moveToBin(item: MediaItem) {
        repository.moveToBin(item)
        if (_viewerMediaList.value.any { it.id == item.id }) {
            closeViewer()
        }
    }

    private fun getSelectedItems(items: List<MediaItem>): List<MediaItem> {
        val selected = _selectedMediaIds.value
        return if (selected.isEmpty()) emptyList() else items.filter { it.id in selected }
    }

    fun moveToBinSelectedItems(items: List<MediaItem>) {
        val itemsToTrash = getSelectedItems(items)
        if (itemsToTrash.isNotEmpty()) {
            repository.moveMultipleToBin(itemsToTrash)
        }
        clearSelection()
    }

    fun shareSelectedItems(context: Context, items: List<MediaItem>) {
        val itemsToShare = getSelectedItems(items)
        if (itemsToShare.isEmpty()) return

        if (itemsToShare.size == 1) {
            val item = itemsToShare.first()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media"))
        } else {
            val uris = ArrayList(itemsToShare.map { it.uri })
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${itemsToShare.size} items"))
        }
        clearSelection()
    }

    fun copySelectedItemsToFolder(items: List<MediaItem>, targetFolder: MediaFolder) {
        val itemsToCopy = getSelectedItems(items)
        if (itemsToCopy.isEmpty()) return
        viewModelScope.launch {
            repository.copyItemsToFolder(itemsToCopy, targetFolder)
            clearSelection()
        }
    }

    fun moveSelectedItemsToFolder(items: List<MediaItem>, targetFolder: MediaFolder) {
        val itemsToMove = getSelectedItems(items)
        if (itemsToMove.isEmpty()) return
        viewModelScope.launch {
            repository.moveItemsToFolder(itemsToMove, targetFolder)
            clearSelection()
        }
    }

    fun restoreFromBin(item: MediaItem) {
        repository.restoreFromBin(item)
    }

    fun permanentlyDelete(item: MediaItem) {
        requestPermanentDelete(listOf(item))
    }

    fun emptyBin() {
        val currentBin = repository.binMedia.value
        if (currentBin.isNotEmpty()) {
            requestPermanentDelete(currentBin)
        }
    }

    private fun requestPermanentDelete(items: List<MediaItem>) {
        viewModelScope.launch {
            val req = repository.preparePermanentDelete(items)
            if (req.intentSender != null) {
                pendingConsentItems = req.itemsToFinalize
                _pendingDeleteRequest.value = IntentSenderRequest.Builder(req.intentSender).build()
            } else {
                pendingConsentItems = emptyList()
            }
        }
    }

    fun onPermanentDeleteResult(confirmed: Boolean) {
        _pendingDeleteRequest.value = null
        if (confirmed && pendingConsentItems.isNotEmpty()) {
            val ids = pendingConsentItems.map { it.id }.toSet()
            pendingConsentItems = emptyList()
            viewModelScope.launch {
                repository.finalizePermanentDelete(ids)
            }
        } else {
            pendingConsentItems = emptyList()
        }
    }

    fun openExternalUri(uri: android.net.Uri, mimeType: String? = null) {
        val isVideo = mimeType?.startsWith("video/") == true ||
                      uri.toString().contains(".mp4", ignoreCase = true) ||
                      uri.toString().contains(".mkv", ignoreCase = true) ||
                      uri.toString().contains(".webm", ignoreCase = true)
        val name = uri.lastPathSegment ?: if (isVideo) "Video" else "Image"
        val tempItem = MediaItem(
            id = System.currentTimeMillis(),
            uri = uri,
            name = name,
            mimeType = mimeType ?: if (isVideo) "video/*" else "image/*",
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis(),
            sizeBytes = 0L,
            width = 0,
            height = 0,
            durationMs = 0L,
            bucketId = "external",
            bucketName = "External",
            relativePath = "",
            isFavorite = false,
            isVideo = isVideo
        )
        _viewerInitialIndex.value = 0
        _viewerMediaList.value = listOf(tempItem)
    }

    fun refresh() {
        repository.refresh()
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
