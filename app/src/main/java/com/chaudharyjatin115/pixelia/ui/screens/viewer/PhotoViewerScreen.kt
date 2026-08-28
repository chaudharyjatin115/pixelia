package com.chaudharyjatin115.pixelia.ui.screens.viewer

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import com.chaudharyjatin115.pixelia.ui.components.MediaInfoSheet
import com.chaudharyjatin115.pixelia.ui.screens.editor.PhotoEditorScreen
import com.chaudharyjatin115.pixelia.ui.theme.FrostedGlassDefaults
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMoveToBin: (MediaItem) -> Unit,
    onImageEdited: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (mediaList.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val viewerHazeState = remember { HazeState() }
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()
    DisposableEffect(isDarkTheme) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    val safeIndex = initialIndex.coerceIn(0, mediaList.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { mediaList.size }
    )

    val currentMedia = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()

    var controlsVisible by remember { mutableStateOf(true) }
    var isCurrentPhotoZoomed by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showEditorScreen by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .hazeSource(state = viewerHazeState)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(180)),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(140))
            ) {
                TopPillHeader(
                    title = currentMedia.name,
                    subtitle = formatHeaderDate(currentMedia.dateTaken),
                    onBackClick = onClose,
                    onInfoClick = { showInfoSheet = true },
                    hazeState = viewerHazeState
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isCurrentPhotoZoomed,
                    modifier = Modifier.fillMaxSize(),
                    key = { mediaList.getOrNull(it)?.id ?: it }
                ) { page ->
                    val item = mediaList.getOrNull(page) ?: return@HorizontalPager
                    val isCurrent = page == pagerState.currentPage
                    ZoomableMediaView(
                        item = item,
                        isActivePage = isCurrent,
                        onSingleTap = { controlsVisible = !controlsVisible },
                        onZoomStateChanged = { zoomed ->
                            if (isCurrent) isCurrentPhotoZoomed = zoomed
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(180)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(140))
            ) {
                BottomPillBar(
                    mediaItem = currentMedia,
                    onShareClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = currentMedia.mimeType.ifEmpty { "image/*" }
                            putExtra(Intent.EXTRA_STREAM, currentMedia.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        shareLauncher.launch(Intent.createChooser(intent, "Share media"))
                    },
                    onEditClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEditorScreen = true
                    },
                    onFavoriteClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite(currentMedia)
                    },
                    onDeleteClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirmDialog = true
                    },
                    hazeState = viewerHazeState
                )
            }
        }
    }

    if (showInfoSheet) {
        MediaInfoSheet(
            mediaItem = currentMedia,
            onDismissRequest = { showInfoSheet = false }
        )
    }

    if (showEditorScreen) {
        PhotoEditorScreen(
            mediaItem = currentMedia,
            onDismiss = { showEditorScreen = false },
            onSaveSuccess = {
                showEditorScreen = false
                onImageEdited()
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Move to Bin?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Move \"${currentMedia.name}\" to the Bin?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onMoveToBin(currentMedia)
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
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TopPillHeader(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    hazeState: HazeState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = CircleShape
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .hazeEffect(state = hazeState, style = FrostedGlassDefaults.photoViewerStyle()),
            shape = shape,
            color = FrostedGlassDefaults.photoViewerBackground(),
            border = FrostedGlassDefaults.photoViewerBorder(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Info",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomPillBar(
    mediaItem: MediaItem,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    hazeState: HazeState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = CircleShape
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 460.dp)
                .height(56.dp)
                .clip(shape)
                .hazeEffect(state = hazeState, style = FrostedGlassDefaults.photoViewerStyle()),
            shape = shape,
            color = FrostedGlassDefaults.photoViewerBackground(),
            border = FrostedGlassDefaults.photoViewerBorder(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }

                if (!mediaItem.isVideo) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (mediaItem.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (mediaItem.isFavorite) MaterialTheme.colorScheme.error else Color.White
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableMediaView(
    item: MediaItem,
    isActivePage: Boolean,
    onSingleTap: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isActivePage) {
        if (!isActivePage) {
            scale = 1f
            offset = Offset.Zero
            onZoomStateChanged(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = newScale
                    offset = if (scale > 1f) offset + pan else Offset.Zero
                    onZoomStateChanged(scale > 1.05f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .clickable(
                    onClick = onSingleTap
                )
        )
    }
}

private fun formatHeaderDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(timestamp))
}
