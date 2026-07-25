package com.chaudharyjatin115.pixelia.data.repository

import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.chaudharyjatin115.pixelia.data.media.MediaStoreScanner
import com.chaudharyjatin115.pixelia.domain.model.DateGroupedMedia
import com.chaudharyjatin115.pixelia.domain.model.MediaFolder
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val STORAGE_UUID_REGEX = Regex("^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$")

data class PermanentDeleteRequest(
    val intentSender: IntentSender? = null,
    val itemsToFinalize: List<MediaItem> = emptyList()
)

class MediaRepository(
    private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val scanner = MediaStoreScanner(context)
    private val localState = LocalMediaStateStore(context)

    private val _rawMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMedia: StateFlow<List<MediaItem>> = _allMedia.asStateFlow()

    private val _groupedMedia = MutableStateFlow<List<DateGroupedMedia>>(emptyList())
    val groupedMedia: StateFlow<List<DateGroupedMedia>> = _groupedMedia.asStateFlow()

    private val _folders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val folders: StateFlow<List<MediaFolder>> = _folders.asStateFlow()

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()

    private val _binMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val binMedia: StateFlow<List<MediaItem>> = _binMedia.asStateFlow()

    private var contentObserver: ContentObserver? = null
    private var refreshJob: kotlinx.coroutines.Job? = null

    init {
        registerMediaStoreObserver()
        refresh()
    }

    private fun registerMediaStoreObserver() {
        val handler = Handler(Looper.getMainLooper())
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                debounceRefresh()
            }
        }
        try {
            contentObserver?.let { observer ->
                context.contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                context.contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
            }
        } catch (e: SecurityException) {
            Log.w("MediaRepository", "Observer registration denied due to missing permission", e)
        } catch (e: IllegalStateException) {
            Log.e("MediaRepository", "ContentResolver state error during observer registration", e)
        }
    }

    private fun debounceRefresh() {
        refreshJob?.cancel()
        refreshJob = externalScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(400L)
            refreshInternal()
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = externalScope.launch(Dispatchers.IO) {
            refreshInternal()
        }
    }

    private suspend fun refreshInternal() {
        _isLoading.value = true
        try {
            val fastItems = scanner.queryFastInitialMedia(limit = 300)
            if (fastItems.isNotEmpty()) {
                _rawMedia.value = fastItems
                applyStateAndPublish(fastItems)
            }

            val fullItems = scanner.queryAllMedia()
            _rawMedia.value = fullItems
            applyStateAndPublish(fullItems)
        } catch (e: SecurityException) {
            Log.w("MediaRepository", "SecurityException while scanning MediaStore", e)
        } catch (e: IllegalStateException) {
            Log.e("MediaRepository", "IllegalStateException while scanning MediaStore", e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun applyStateAndPublish(scannedItems: List<MediaItem>) {
        val favIds = localState.getFavoriteIds()
        val unfavIds = localState.getUnfavoriteIds()
        val trashedIds = localState.getTrashedIds()

        val processed = ArrayList<MediaItem>(scannedItems.size)
        val activeMedia = ArrayList<MediaItem>(scannedItems.size)
        val binList = ArrayList<MediaItem>()
        val favList = ArrayList<MediaItem>()

        val hasOverrides = favIds.isNotEmpty() || unfavIds.isNotEmpty() || trashedIds.isNotEmpty()
        if (!hasOverrides) {
            for (item in scannedItems) {
                processed.add(item)
                activeMedia.add(item)
                if (item.isFavorite) {
                    favList.add(item)
                }
            }
        } else {
            val favIdSet = favIds.mapNotNullTo(HashSet()) { it.toLongOrNull() }
            val unfavIdSet = unfavIds.mapNotNullTo(HashSet()) { it.toLongOrNull() }
            val trashedIdSet = trashedIds.mapNotNullTo(HashSet()) { it.toLongOrNull() }

            for (item in scannedItems) {
                val isFav = when {
                    favIdSet.contains(item.id) -> true
                    unfavIdSet.contains(item.id) -> false
                    else -> item.isFavorite
                }

                // An item is ONLY trashed if explicitly moved to Bin in our app
                val isTrash = trashedIdSet.contains(item.id)
                val trashTime = if (isTrash) {
                    val localTime = localState.getTrashTimestamp(item.id.toString())
                    if (localTime > 0) localTime else item.dateModified
                } else 0L

                val itemWithState = if (isFav != item.isFavorite || isTrash != item.isTrashed || trashTime != item.trashedTime) {
                    item.copy(
                        isFavorite = isFav,
                        isTrashed = isTrash,
                        trashedTime = trashTime
                    )
                } else {
                    item
                }
                processed.add(itemWithState)

                if (isTrash) {
                    binList.add(itemWithState)
                } else {
                    activeMedia.add(itemWithState)
                    if (isFav) {
                        favList.add(itemWithState)
                    }
                }
            }
        }

        _allMedia.value = activeMedia
        _favorites.value = favList
        _binMedia.value = binList
        _groupedMedia.value = groupMediaByDate(activeMedia)
        _folders.value = groupMediaByFolder(activeMedia)
    }

    private fun groupMediaByDate(items: List<MediaItem>): List<DateGroupedMedia> {
        return items.groupBy { item ->
            val ts = if (item.dateTaken > 0) item.dateTaken else item.dateModified
            formatDateKey(ts)
        }.map { (dateKey, groupedItems) ->
            val firstTs = groupedItems.firstOrNull()?.let { if (it.dateTaken > 0) it.dateTaken else it.dateModified } ?: 0L
            DateGroupedMedia(
                header = formatDateHeader(firstTs),
                dateKey = dateKey,
                items = groupedItems
            )
        }
    }

    private fun isDefaultPhoneCamera(item: MediaItem): Boolean {
        val rel = item.relativePath.trim().replace('\\', '/')
        val name = item.bucketName.trim()

        if (rel.isNotBlank()) {
            val cleanRel = rel.trim('/')
            val segments = cleanRel.split('/').filter { it.isNotBlank() }
            val cameraIdx = segments.indexOfLast { it.equals("Camera", ignoreCase = true) }
            if (cameraIdx != -1) {
                // If there are subdirectories after Camera (e.g. DCIM/Camera/Raw), it's a subfolder
                if (cameraIdx < segments.size - 1) {
                    return false
                }
                // Check if preceded by DCIM
                val dcimIdx = segments.indexOfLast { it.equals("DCIM", ignoreCase = true) }
                if (dcimIdx != -1 && dcimIdx < cameraIdx) {
                    return true
                }
            }
        }

        val isDcim = rel.startsWith("DCIM", ignoreCase = true) || rel.contains("/DCIM", ignoreCase = true)
        return (isDcim && name.equals("Camera", ignoreCase = true)) ||
               (isDcim && item.bucketId.equals("camera", ignoreCase = true))
    }

    private fun isMainPicturesFolder(item: MediaItem): Boolean {
        if (isDefaultPhoneCamera(item)) return false

        val rel = item.relativePath.trim().replace('\\', '/')
        val name = item.bucketName.trim()

        if (rel.isNotBlank()) {
            val cleanRel = rel.trim('/')
            val segments = cleanRel.split('/').filter { it.isNotBlank() }
            val picturesIdx = segments.indexOfLast { it.equals("Pictures", ignoreCase = true) }
            if (picturesIdx != -1) {
                // If there are segments after "Pictures", this is a subfolder inside Pictures (e.g. Pictures/Screenshots)
                if (picturesIdx < segments.size - 1) {
                    return false
                }
                // If Pictures is the first segment (standard scoped storage: "Pictures/"), it's the root Pictures folder
                if (picturesIdx == 0) {
                    return true
                }
                // If it's a full path (e.g. /storage/emulated/0/Pictures), ensure Pictures is directly under the storage root
                val parentSegment = segments[picturesIdx - 1]
                val isStorageRoot = parentSegment == "0" ||
                    parentSegment.equals("emulated", ignoreCase = true) ||
                    parentSegment.equals("storage", ignoreCase = true) ||
                    parentSegment.equals("sdcard", ignoreCase = true) ||
                    parentSegment.matches(STORAGE_UUID_REGEX)
                if (isStorageRoot) {
                    return true
                }
                return false
            }
        }

        // Fallback when relativePath is not available: check bucketName
        return name.equals("Pictures", ignoreCase = true)
    }

    private fun groupMediaByFolder(items: List<MediaItem>): List<MediaFolder> {
        val folderMap = LinkedHashMap<String, MutableList<MediaItem>>()
        for (item in items) {
            val isCamera = isDefaultPhoneCamera(item)
            val isMainPictures = !isCamera && isMainPicturesFolder(item)
            val bucketKey = when {
                isCamera -> "camera_default_unified"
                isMainPictures -> "pictures_default_unified"
                item.bucketId.isNotBlank() -> item.bucketId
                item.relativePath.isNotBlank() -> item.relativePath.trim().replace('\\', '/').trim('/')
                else -> item.bucketName.ifBlank { "Photos" }
            }
            val list = folderMap.getOrPut(bucketKey) { mutableListOf() }
            list.add(item)
        }

        return folderMap.map { (key, folderItems) ->
            val latest = folderItems.maxByOrNull { it.dateTaken } ?: folderItems.first()
            val isCamera = key == "camera_default_unified"
            val isPictures = key == "pictures_default_unified"
            val bucketName = when {
                isCamera -> "Camera"
                isPictures -> "Pictures"
                latest.bucketName.isNotBlank() && !latest.bucketName.equals("Photos", ignoreCase = true) -> {
                    if (latest.bucketName.equals("Camera", ignoreCase = true)) {
                        val parentFolder = latest.relativePath.trim().replace('\\', '/').trim('/').substringBeforeLast('/', "")
                        if (parentFolder.isNotBlank()) "$parentFolder/Camera" else "Camera (Other)"
                    } else if (latest.bucketName.equals("Pictures", ignoreCase = true)) {
                        val parentFolder = latest.relativePath.trim().replace('\\', '/').trim('/').substringBeforeLast('/', "")
                        if (parentFolder.isNotBlank()) "$parentFolder/Pictures" else "Pictures (Subfolder)"
                    } else {
                        latest.bucketName
                    }
                }
                latest.relativePath.isNotBlank() -> {
                    val folderName = latest.relativePath.trim().replace('\\', '/').trim('/').substringAfterLast('/')
                    folderName.ifBlank { "Photos" }
                }
                else -> "Photos"
            }
            val bucketId = when {
                isCamera -> "camera_default_unified"
                isPictures -> "pictures_default_unified"
                else -> key
            }
            val totalSize = folderItems.sumOf { it.sizeBytes }
            MediaFolder(
                bucketId = bucketId,
                bucketName = bucketName,
                coverUri = latest.uri,
                mediaCount = folderItems.size,
                latestDateTaken = folderItems.maxOfOrNull { it.dateTaken } ?: 0L,
                latestDateModified = folderItems.maxOfOrNull { it.dateModified } ?: 0L,
                totalSizeBytes = totalSize
            )
        }.sortedWith(
            compareByDescending<MediaFolder> { it.bucketId == "camera_default_unified" }
                .thenByDescending { it.bucketId == "pictures_default_unified" }
                .thenByDescending { it.mediaCount }
        )
    }

    fun toggleFavorite(item: MediaItem) {
        val newFav = !item.isFavorite
        localState.setFavorite(item.id.toString(), newFav)
        _rawMedia.value = _rawMedia.value.map {
            if (it.id == item.id) it.copy(isFavorite = newFav) else it
        }
        applyStateAndPublish(_rawMedia.value)
    }

    fun moveToBin(item: MediaItem) = moveMultipleToBin(listOf(item))

    fun moveMultipleToBin(items: Collection<MediaItem>) {
        if (items.isEmpty()) return
        val idsToTrash = items.map { it.id.toString() }
        localState.batchMoveToTrash(idsToTrash)
        val idSet = items.map { it.id }.toSet()
        val now = System.currentTimeMillis()
        _rawMedia.value = _rawMedia.value.map {
            if (idSet.contains(it.id)) it.copy(isTrashed = true, trashedTime = now) else it
        }
        applyStateAndPublish(_rawMedia.value)
    }

    fun restoreFromBin(item: MediaItem) = restoreMultipleFromBin(listOf(item))

    fun restoreMultipleFromBin(items: Collection<MediaItem>) {
        if (items.isEmpty()) return
        val idsToRestore = items.map { it.id.toString() }
        localState.batchRestoreFromTrash(idsToRestore)
        val idSet = items.map { it.id }.toSet()
        _rawMedia.value = _rawMedia.value.map {
            if (idSet.contains(it.id)) it.copy(isTrashed = false, trashedTime = 0L) else it
        }
        applyStateAndPublish(_rawMedia.value)
    }

    suspend fun copyItemsToFolder(items: List<MediaItem>, targetFolder: MediaFolder): Boolean {
        return withContext(Dispatchers.IO) {
            var success = false
            for (item in items) {
                try {
                    val isVideo = item.isVideo
                    val collectionUri = if (isVideo) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }

                    val cleanFolder = (if (targetFolder.bucketName.isNotBlank()) targetFolder.bucketName else "Pictures")
                        .replace("..", "").replace("/", "_").replace("\\", "_").trim()
                    val folderSubdir = cleanFolder.ifBlank { "Pictures" }
                    val safeFileName = item.name.replace("..", "").replace("/", "_").replace("\\", "_").trim()
                    val relativePath = when (targetFolder.bucketId) {
                        "pictures_default_unified" -> {
                            if (isVideo) android.os.Environment.DIRECTORY_MOVIES else android.os.Environment.DIRECTORY_PICTURES
                        }
                        "camera_default_unified" -> {
                            "${android.os.Environment.DIRECTORY_DCIM}/Camera"
                        }
                        else -> {
                            if (isVideo) {
                                "${android.os.Environment.DIRECTORY_MOVIES}/$folderSubdir"
                            } else {
                                "${android.os.Environment.DIRECTORY_PICTURES}/$folderSubdir"
                            }
                        }
                    }

                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "copy_${System.currentTimeMillis()}_${safeFileName}")
                        put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val newUri = context.contentResolver.insert(collectionUri, values)
                    if (newUri != null) {
                        context.contentResolver.openInputStream(item.uri)?.use { input ->
                            context.contentResolver.openOutputStream(newUri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            context.contentResolver.update(newUri, values, null, null)
                        }
                        success = true
                    }
                } catch (e: IOException) {
                    Log.e("MediaRepository", "Failed to copy media item ${item.id}", e)
                } catch (e: SecurityException) {
                    Log.w("MediaRepository", "Permission denied copying media item ${item.id}", e)
                } catch (e: IllegalArgumentException) {
                    Log.e("MediaRepository", "Invalid arguments for copying media item ${item.id}", e)
                }
            }
            refresh()
            success
        }
    }

    suspend fun moveItemsToFolder(items: List<MediaItem>, targetFolder: MediaFolder): Boolean {
        return withContext(Dispatchers.IO) {
            val copied = copyItemsToFolder(items, targetFolder)
            if (copied) {
                for (item in items) {
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (e: SecurityException) {
                        Log.w("MediaRepository", "SecurityException deleting moved item ${item.id}; soft-trashing locally", e)
                        localState.moveToTrash(item.id.toString())
                    } catch (e: IllegalArgumentException) {
                        Log.e("MediaRepository", "Invalid URI deleting moved item ${item.id}", e)
                        localState.moveToTrash(item.id.toString())
                    }
                }
                refresh()
            }
            copied
        }
    }

    suspend fun preparePermanentDelete(items: List<MediaItem>): PermanentDeleteRequest = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext PermanentDeleteRequest()

        val directlyDeletedItems = mutableListOf<MediaItem>()
        val itemsNeedingConsent = mutableListOf<MediaItem>()

        for (item in items) {
            try {
                val rows = context.contentResolver.delete(item.uri, null, null)
                if (rows > 0) {
                    directlyDeletedItems.add(item)
                } else {
                    itemsNeedingConsent.add(item)
                }
            } catch (e: SecurityException) {
                itemsNeedingConsent.add(item)
            } catch (e: IllegalArgumentException) {
                itemsNeedingConsent.add(item)
            } catch (e: IllegalStateException) {
                itemsNeedingConsent.add(item)
            }
        }

        if (directlyDeletedItems.isNotEmpty()) {
            finalizePermanentDelete(directlyDeletedItems.map { it.id }.toSet())
        }

        if (itemsNeedingConsent.isEmpty()) {
            return@withContext PermanentDeleteRequest()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uris = itemsNeedingConsent.map { it.uri }
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                return@withContext PermanentDeleteRequest(
                    intentSender = pendingIntent.intentSender,
                    itemsToFinalize = itemsNeedingConsent
                )
            } catch (e: IllegalArgumentException) {
                Log.e("MediaRepository", "Failed to create MediaStore delete request", e)
            } catch (e: IllegalStateException) {
                Log.e("MediaRepository", "Illegal state creating MediaStore delete request", e)
            }
        }

        return@withContext PermanentDeleteRequest(itemsToFinalize = itemsNeedingConsent)
    }

    suspend fun finalizePermanentDelete(deletedIds: Set<Long>) = withContext(Dispatchers.IO) {
        if (deletedIds.isEmpty()) return@withContext
        val idStrings = deletedIds.map { it.toString() }
        localState.removeMediaIds(idStrings)
        _rawMedia.value = _rawMedia.value.filterNot { deletedIds.contains(it.id) }
        applyStateAndPublish(_rawMedia.value)
        refresh()
    }

    fun permanentlyDelete(item: MediaItem) {
        externalScope.launch(Dispatchers.IO) {
            preparePermanentDelete(listOf(item))
        }
    }

    fun emptyBin() {
        externalScope.launch(Dispatchers.IO) {
            preparePermanentDelete(_binMedia.value)
        }
    }

    fun getItemsInFolder(bucketId: String): List<MediaItem> {
        if (bucketId == "camera_default_unified") {
            return _allMedia.value.filter { isDefaultPhoneCamera(it) }
        }
        if (bucketId == "pictures_default_unified") {
            return _allMedia.value.filter { isMainPicturesFolder(it) }
        }
        return _allMedia.value.filter {
            if (isDefaultPhoneCamera(it) || isMainPicturesFolder(it)) return@filter false
            it.bucketId == bucketId ||
                it.relativePath.trim().replace('\\', '/').trim('/') == bucketId ||
                (it.bucketId.isBlank() && it.bucketName.equals(bucketId, ignoreCase = true))
        }
    }

    private val zoneId = ZoneId.systemDefault()
    private val headerMonthDay = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    private val headerFull = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

    private fun formatDateHeader(timestamp: Long): String {
        return try {
            val date = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            val today = LocalDate.now(zoneId)
            when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                date.year == today.year -> date.format(headerMonthDay)
                else -> date.format(headerFull)
            }
        } catch (e: DateTimeException) {
            "Recent"
        }
    }

    private fun formatDateKey(timestamp: Long): String {
        return try {
            Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate().toString()
        } catch (e: DateTimeException) {
            timestamp.toString()
        }
    }

    fun cleanup() {
        contentObserver?.let { observer ->
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: SecurityException) {
                Log.w("MediaRepository", "Permission error unregistering ContentObserver", e)
            } catch (e: IllegalStateException) {
                Log.e("MediaRepository", "IllegalStateException unregistering ContentObserver", e)
            }
        }
        contentObserver = null
    }
}
