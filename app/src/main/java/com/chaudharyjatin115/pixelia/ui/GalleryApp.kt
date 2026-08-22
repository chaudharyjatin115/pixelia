package com.chaudharyjatin115.pixelia.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.chaudharyjatin115.pixelia.ui.components.FolderPickerDialog
import com.chaudharyjatin115.pixelia.ui.components.PermissionRationaleScreen
import com.chaudharyjatin115.pixelia.ui.navigation.FolderAction
import com.chaudharyjatin115.pixelia.ui.navigation.FolderActionBar
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryDestination
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryNavigationBar
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryNavigationRail
import com.chaudharyjatin115.pixelia.ui.screens.folders.FolderDetailScreen
import com.chaudharyjatin115.pixelia.ui.screens.folders.FoldersScreen
import com.chaudharyjatin115.pixelia.ui.screens.photos.PhotosScreen
import com.chaudharyjatin115.pixelia.viewmodel.GalleryViewModel

@Composable
fun GalleryApp(
    initialViewUri: android.net.Uri? = null,
    initialViewMimeType: String? = null,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(initialViewUri) {
        if (initialViewUri != null) {
            viewModel.openExternalUri(initialViewUri, initialViewMimeType)
        }
    }
    val hazeState = remember { HazeState() }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840 && configuration.screenHeightDp >= 600

    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val isPartialAccess by viewModel.isPartialAccess.collectAsState()
    val activeFolder by viewModel.activeFolder.collectAsState()

    val selectedMediaIds by viewModel.selectedMediaIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val activeFolderAction by viewModel.activeFolderAction.collectAsState()
    var showFolderPickerForAction by remember { mutableStateOf<FolderAction?>(null) }

    val groupedMedia by viewModel.groupedMedia.collectAsState()
    val allMedia by viewModel.allMedia.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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

    LaunchedEffect(Unit) {
        checkPermissions()
    }

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

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasStoragePermission) {
            PermissionRationaleScreen(
                onRequestPermission = { requestPermissions() }
            )
        } else {
            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    GalleryNavigationRail(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { dest ->
                            viewModel.selectDestination(dest)
                        },
                        hazeState = hazeState
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        MainContentScreen(
                            activeFolder = activeFolder,
                            selectedDestination = selectedDestination,
                            viewModel = viewModel,
                            allMedia = allMedia,
                            groupedMedia = groupedMedia,
                            folders = folders,
                            isLoading = isLoading,
                            isPartialAccess = isPartialAccess,
                            isSelectionMode = isSelectionMode,
                            selectedMediaIds = selectedMediaIds,
                            onRequestPermissions = { requestPermissions() },
                            hazeState = hazeState
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .hazeSource(state = hazeState)
                    ) {
                        MainContentScreen(
                            activeFolder = activeFolder,
                            selectedDestination = selectedDestination,
                            viewModel = viewModel,
                            allMedia = allMedia,
                            groupedMedia = groupedMedia,
                            folders = folders,
                            isLoading = isLoading,
                            isPartialAccess = isPartialAccess,
                            isSelectionMode = isSelectionMode,
                            selectedMediaIds = selectedMediaIds,
                            onRequestPermissions = { requestPermissions() },
                            hazeState = hazeState
                        )
                    }

                    val isFolderOrSelection = activeFolder != null || isSelectionMode
                    val currentFolder = activeFolder
                    val activeFolderItems = remember(currentFolder, allMedia) {
                        if (currentFolder != null) viewModel.getItemsForFolder(currentFolder) else emptyList()
                    }
                    val currentTargetItems = if (activeFolder != null) {
                        activeFolderItems
                    } else {
                        allMedia
                    }

                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(tween(200)),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut(tween(160)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        if (isFolderOrSelection) {
                            FolderActionBar(
                                activeAction = activeFolderAction,
                                onActionClick = { action ->
                                    viewModel.setActiveFolderAction(action)
                                    if (selectedMediaIds.isEmpty()) {
                                        viewModel.startSelectionMode()
                                    } else {
                                        when (action) {
                                            FolderAction.SHARE -> {
                                                viewModel.shareSelectedItems(context, currentTargetItems)
                                            }
                                            FolderAction.DELETE -> {
                                                viewModel.moveToBinSelectedItems(currentTargetItems)
                                            }
                                            FolderAction.COPY -> {
                                                showFolderPickerForAction = FolderAction.COPY
                                            }
                                            FolderAction.MOVE -> {
                                                showFolderPickerForAction = FolderAction.MOVE
                                            }
                                        }
                                    }
                                },
                                hazeState = hazeState
                            )
                        } else {
                            GalleryNavigationBar(
                                selectedDestination = selectedDestination,
                                onDestinationSelected = { dest ->
                                    viewModel.selectDestination(dest)
                                },
                                hazeState = hazeState
                            )
                        }
                    }
                }
            }
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
                onDismissRequest = {
                    showFolderPickerForAction = null
                    viewModel.setActiveFolderAction(FolderAction.SHARE)
                }
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
    isLoading: Boolean,
    isPartialAccess: Boolean,
    isSelectionMode: Boolean,
    selectedMediaIds: Set<Long>,
    onRequestPermissions: () -> Unit,
    hazeState: HazeState
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
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = {
                    if (selectedMediaIds.size == folderItems.size) {
                        viewModel.clearSelection()
                    } else {
                        viewModel.selectAll(folderItems)
                    }
                },
                hazeState = hazeState
            )
        } else {
            AnimatedContent(
                targetState = selectedDestination,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) +
                     scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)))
                        .togetherWith(fadeOut(animationSpec = tween(160)))
                },
                label = "tab_content_transition"
            ) { destination ->
                when (destination) {
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
                            onClearSelection = { viewModel.clearSelection() },
                            onSelectAll = {
                                if (selectedMediaIds.size == allMedia.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll(allMedia)
                                }
                            },
                            onRefresh = { viewModel.refresh() },
                            onManagePermissions = onRequestPermissions,
                            hazeState = hazeState
                        )
                    }
                    GalleryDestination.FOLDERS -> {
                        FoldersScreen(
                            folders = folders,
                            allMedia = allMedia,
                            getItemsForFolder = { folder -> viewModel.getItemsForFolder(folder) },
                            onFolderClick = { folder -> viewModel.openFolder(folder) },
                            onPhotoClick = { item, list -> viewModel.openViewer(item, list) },
                            hazeState = hazeState
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
