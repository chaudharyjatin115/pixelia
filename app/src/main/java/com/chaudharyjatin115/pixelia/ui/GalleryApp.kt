package com.chaudharyjatin115.pixelia.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.chaudharyjatin115.pixelia.ui.components.PermissionRationaleScreen
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryDestination
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryNavigationBar
import com.chaudharyjatin115.pixelia.ui.navigation.GalleryNavigationRail
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

    val selectedMediaIds by viewModel.selectedMediaIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    val groupedMedia by viewModel.groupedMedia.collectAsState()
    val allMedia by viewModel.allMedia.collectAsState()
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

    if (isSelectionMode) {
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
                            onManagePermissions = { requestPermissions() },
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
                            onManagePermissions = { requestPermissions() },
                            hazeState = hazeState
                        )
                    }

                    GalleryNavigationBar(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { dest ->
                            viewModel.selectDestination(dest)
                        },
                        hazeState = hazeState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
