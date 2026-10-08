package com.chaudharyjatin115.pixelia.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalConfiguration
import com.chaudharyjatin115.pixelia.ui.components.FolderPickerDialog
import com.chaudharyjatin115.pixelia.ui.components.PermissionRationaleScreen
import com.chaudharyjatin115.pixelia.ui.navigation.FloatingPillBar
import com.chaudharyjatin115.pixelia.ui.navigation.FolderAction
import com.chaudharyjatin115.pixelia.ui.navigation.FolderActionBar
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryDestination
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryNavigationBar
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryNavigationRail
import com.chaudharyjatin115.pixelia.ui.screens.bin.BinScreen
import com.chaudharyjatin115.pixelia.ui.screens.favourites.FavouritesScreen
import com.chaudharyjatin115.pixelia.ui.screens.folders.FolderDetailScreen
import com.chaudharyjatin115.pixelia.ui.screens.folders.FoldersScreen
import com.chaudharyjatin115.pixelia.ui.screens.photos.PhotosScreen
import com.chaudharyjatin115.pixelia.ui.screens.viewer.PhotoViewerScreen
import com.chaudharyjatin115.pixelia.viewmodel.GalleryViewModel

@Composable
fun GalleryApp(
    modifier: Modifier = Modifier,
    initialViewUri: Uri? = null,
    initialViewMimeType: String? = null,
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val isPartialAccess by viewModel.isPartialAccess.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val activeFolder by viewModel.activeFolder.collectAsState()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val activeFolderAction by viewModel.activeFolderAction.collectAsState()
    var showFolderPickerForAction by remember { mutableStateOf<FolderAction?>(null) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

    val isViewerOpen by viewModel.isViewerOpen.collectAsState()
    val viewerMediaList by viewModel.viewerMediaList.collectAsState()
    val viewerInitialIndex by viewModel.viewerInitialIndex.collectAsState()

    val groupedMedia by viewModel.groupedMedia.collectAsState()
    val allMedia by viewModel.allMedia.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val binMedia by viewModel.binMedia.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(initialViewUri) {
        if (initialViewUri != null) {
            viewModel.openExternalUri(initialViewUri, initialViewMimeType)
        }
    }

    val pendingDeleteRequest by viewModel.pendingDeleteRequest.collectAsState()
    val systemDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onPermanentDeleteResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(pendingDeleteRequest) {
        pendingDeleteRequest?.let { req ->
            systemDeleteLauncher.launch(req)
        }
    }

    // Permission check helper
    fun checkPermissions() {
        val hasFull = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }

        val hasPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        viewModel.updatePermissionState(hasFull = hasFull, hasPartial = hasPartial)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    fun requestPermissions() {
        val permissionsToRequest = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    if (!isViewerOpen) {
        if (activeFolder != null) {
            BackHandler {
                if (isSelectionMode) {
                    viewModel.clearSelection()
                } else {
                    viewModel.closeFolder()
                }
            }
        } else if (isSelectionMode) {
            BackHandler {
                viewModel.clearSelection()
            }
        } else if (selectedDestination != GalleryDestination.PHOTOS) {
            BackHandler {
                viewModel.selectDestination(GalleryDestination.PHOTOS)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasStoragePermission) {
            PermissionRationaleScreen(
                onRequestPermission = { requestPermissions() }
            )
        } else {
            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isViewerOpen) {
                        GalleryNavigationRail(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { dest ->
                                viewModel.selectDestination(dest)
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        if (!isViewerOpen) {
                            MainContentScreen(
                                activeFolder = activeFolder,
                                selectedDestination = selectedDestination,
                                viewModel = viewModel,
                                allMedia = allMedia,
                                groupedMedia = groupedMedia,
                                folders = folders,
                                favorites = favorites,
                                binMedia = binMedia,
                                isLoading = isLoading,
                                isPartialAccess = isPartialAccess,
                                isSelectionMode = isSelectionMode,
                                selectedMediaIds = selectedMediaIds,
                                onRequestPermissions = { requestPermissions() }
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isViewerOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            MainContentScreen(
                                activeFolder = activeFolder,
                                selectedDestination = selectedDestination,
                                viewModel = viewModel,
                                allMedia = allMedia,
                                groupedMedia = groupedMedia,
                                folders = folders,
                                favorites = favorites,
                                binMedia = binMedia,
                                isLoading = isLoading,
                                isPartialAccess = isPartialAccess,
                                isSelectionMode = isSelectionMode,
                                selectedMediaIds = selectedMediaIds,
                                onRequestPermissions = { requestPermissions() }
                            )
                        }
                    }

                    val showMainNavBar = !isViewerOpen && activeFolder == null && !isSelectionMode
                    val showActionPillBar = !isViewerOpen && isSelectionMode

                    AnimatedVisibility(
                        visible = showMainNavBar,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(tween(180)),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut(tween(140)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        GalleryNavigationBar(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { dest ->
                                viewModel.selectDestination(dest)
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = showActionPillBar,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(tween(180)),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut(tween(140)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        FolderActionBar(
                            activeAction = activeFolderAction,
                            onActionClick = { action ->
                                viewModel.setActiveFolderAction(action)
                                val currentTargetItems = if (activeFolder != null) {
                                    viewModel.getItemsForActiveFolder()
                                } else {
                                    allMedia
                                }
                                when (action) {
                                    FolderAction.SHARE -> {
                                        viewModel.shareSelectedItems(context, currentTargetItems)
                                    }
                                    FolderAction.DELETE -> {
                                        showBatchDeleteConfirmDialog = true
                                    }
                                    FolderAction.COPY -> {
                                        showFolderPickerForAction = FolderAction.COPY
                                    }
                                    FolderAction.MOVE -> {
                                        showFolderPickerForAction = FolderAction.MOVE
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showBatchDeleteConfirmDialog) {
            val currentTargetItems = if (activeFolder != null) {
                viewModel.getItemsForActiveFolder()
            } else {
                allMedia
            }
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirmDialog = false },
                title = {
                    Text(
                        text = "Move to Bin?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Move ${selectedMediaIds.size} selected items to the Bin?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBatchDeleteConfirmDialog = false
                            viewModel.moveToBinSelectedItems(currentTargetItems)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Move to Bin")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showFolderPickerForAction != null) {
            val currentTargetItems = if (activeFolder != null) {
                viewModel.getItemsForActiveFolder()
            } else {
                allMedia
            }
            FolderPickerDialog(
                title = if (showFolderPickerForAction == FolderAction.COPY) "Copy to Folder" else "Move to Folder",
                folders = folders,
                onFolderSelected = { targetFolder ->
                    val action = showFolderPickerForAction
                    showFolderPickerForAction = null
                    if (action == FolderAction.COPY) {
                        viewModel.copySelectedItemsToFolder(currentTargetItems, targetFolder)
                    } else if (action == FolderAction.MOVE) {
                        viewModel.moveSelectedItemsToFolder(currentTargetItems, targetFolder)
                    }
                },
                onDismissRequest = { showFolderPickerForAction = null }
            )
        }

        AnimatedVisibility(
            visible = isViewerOpen,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(160)) + scaleOut(
                targetScale = 0.94f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            PhotoViewerScreen(
                mediaList = viewerMediaList,
                initialIndex = viewerInitialIndex,
                onClose = { viewModel.closeViewer() },
                onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                onMoveToBin = { item -> viewModel.moveToBin(item) },
                onImageEdited = { viewModel.refresh() }
            )
        }
    }
}

@Composable
private fun MainContentScreen(
    activeFolder: com.chaudharyjatin115.pixelia.domain.model.MediaFolder?,
    selectedDestination: GalleryDestination,
    viewModel: GalleryViewModel,
    allMedia: List<com.chaudharyjatin115.pixelia.domain.model.MediaItem>,
    groupedMedia: List<com.chaudharyjatin115.pixelia.domain.model.DateGroupedMedia>,
    folders: List<com.chaudharyjatin115.pixelia.domain.model.MediaFolder>,
    favorites: List<com.chaudharyjatin115.pixelia.domain.model.MediaItem>,
    binMedia: List<com.chaudharyjatin115.pixelia.domain.model.MediaItem>,
    isLoading: Boolean,
    isPartialAccess: Boolean,
    isSelectionMode: Boolean,
    selectedMediaIds: Set<Long>,
    onRequestPermissions: () -> Unit
) {
    AnimatedContent(
        targetState = activeFolder,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) +
             scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)))
                .togetherWith(fadeOut(animationSpec = tween(160)))
        },
        label = "folder_content_transition"
    ) { currentFolder ->
        if (currentFolder != null) {
            val folderItems = remember(currentFolder, allMedia) { viewModel.getItemsForFolder(currentFolder) }
            FolderDetailScreen(
                folder = currentFolder,
                items = folderItems,
                isSelectionMode = isSelectionMode,
                selectedMediaIds = selectedMediaIds,
                onBackClick = { viewModel.closeFolder() },
                onPhotoClick = { item, list -> viewModel.openViewer(item, list) },
                onItemClick = { item -> viewModel.toggleSelectMedia(item.id) },
                onItemLongClick = { item -> viewModel.toggleSelectMedia(item.id) },
                onUpdateSelection = { ids -> viewModel.setSelectedMedia(ids) },
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = {
                    if (selectedMediaIds.size == folderItems.size) {
                        viewModel.clearSelection()
                    } else {
                        viewModel.selectAll(folderItems)
                    }
                }
            )
        } else {
            val destinations = remember { GalleryDestination.entries }
            val pagerState = rememberPagerState(
                initialPage = selectedDestination.ordinal,
                pageCount = { destinations.size }
            )

            LaunchedEffect(selectedDestination) {
                if (pagerState.currentPage != selectedDestination.ordinal) {
                    pagerState.animateScrollToPage(selectedDestination.ordinal)
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                val dest = destinations.getOrNull(pagerState.currentPage)
                if (dest != null && dest != selectedDestination) {
                    viewModel.selectDestination(dest)
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isSelectionMode,
                modifier = Modifier.fillMaxSize(),
                key = { destinations[it].name }
            ) { page ->
                when (destinations[page]) {
                    GalleryDestination.PHOTOS -> {
                        PhotosScreen(
                            groupedMedia = groupedMedia,
                            allMedia = allMedia,
                            isLoading = isLoading,
                            isPartialAccess = isPartialAccess,
                            isSelectionMode = isSelectionMode,
                            selectedMediaIds = selectedMediaIds,
                            onPhotoClick = { item, list -> viewModel.openViewer(item, list) },
                            onItemClick = { item -> viewModel.toggleSelectMedia(item.id) },
                            onItemLongClick = { item -> viewModel.toggleSelectMedia(item.id) },
                            onUpdateSelection = { ids -> viewModel.setSelectedMedia(ids) },
                            onClearSelection = { viewModel.clearSelection() },
                            onSelectAll = {
                                if (selectedMediaIds.size == allMedia.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll(allMedia)
                                }
                            },
                            onRefresh = { viewModel.refresh() },
                            onManagePermissions = onRequestPermissions
                        )
                    }
                    GalleryDestination.FOLDERS -> {
                        FoldersScreen(
                            folders = folders,
                            onFolderClick = { folder -> viewModel.openFolder(folder) }
                        )
                    }
                    GalleryDestination.FAVOURITES -> {
                        FavouritesScreen(
                            favourites = favorites,
                            onPhotoClick = { item, list -> viewModel.openViewer(item, list) }
                        )
                    }
                    GalleryDestination.BIN -> {
                        BinScreen(
                            binMedia = binMedia,
                            onRestore = { item -> viewModel.restoreFromBin(item) },
                            onPermanentDelete = { item -> viewModel.permanentlyDelete(item) },
                            onEmptyBin = { viewModel.emptyBin() }
                        )
                    }
                }
            }
        }
    }
}
