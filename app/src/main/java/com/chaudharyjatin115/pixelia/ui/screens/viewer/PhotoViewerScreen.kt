package com.chaudharyjatin115.pixelia.ui.screens.viewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
        val insets = window?.let { WindowCompat.getInsetsController(it, view) }
        insets?.isAppearanceLightStatusBars = false
        insets?.isAppearanceLightNavigationBars = false
        onDispose {
            insets?.isAppearanceLightStatusBars = !isDarkTheme
            insets?.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0)),
        pageCount = { mediaList.size }
    )

    LaunchedEffect(initialIndex) {
        if (mediaList.isNotEmpty() && initialIndex in mediaList.indices) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    val isTabletopMode = rememberTabletopMode()
    var controlsVisible by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showEditorSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isCurrentPhotoZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(isTabletopMode) {
        if (isTabletopMode) {
            controlsVisible = true
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPhotoZoomed = false
    }

    var localFavMap by remember { mutableStateOf(mapOf<Long, Boolean>()) }

    val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()
    val isCurrentFav = localFavMap[currentItem.id] ?: currentItem.isFavorite

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val dismissAnimY = remember { Animatable(0f) }

    val currentOffsetY = dismissAnimY.value
    val dismissProgress = (currentOffsetY / 600f).coerceIn(0f, 1f)
    val contentScale = 1f - (dismissProgress * 0.22f)
    val bgAlpha = (1f - (dismissProgress * 0.85f)).coerceIn(0f, 1f)

    if (isTabletopMode) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
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
                    if (item.isVideo) {
                        VideoPlayerView(
                            item = item,
                            isActivePage = isCurrent,
                            controlsVisible = true,
                            onToggleControls = {}
                        )
                    } else {
                        ZoomableMediaView(
                            item = item,
                            isActivePage = isCurrent,
                            onSingleTap = {},
                            onZoomStateChanged = {}
                        )
                    }
                }
            }

            TabletopControlPanel(
                item = currentItem,
                isFavorite = isCurrentFav,
                onClose = onClose,
                onToggleFavorite = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val nextFav = !isCurrentFav
                    localFavMap = localFavMap + (currentItem.id to nextFav)
                    onToggleFavorite(currentItem)
                },
                onShare = { shareMedia(context, currentItem) },
                onEdit = { showEditorSheet = true },
                onUseAs = { useMediaAs(context, currentItem) },
                onDelete = { showDeleteConfirmDialog = true },
                onShowInfo = { showInfoSheet = true },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = currentOffsetY
                        scaleX = contentScale
                        scaleY = contentScale
                    }
                    .pointerInput(isCurrentPhotoZoomed) {
                        if (!isCurrentPhotoZoomed) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    val newOffset = (dismissAnimY.value + dragAmount).coerceAtLeast(0f)
                                    if (newOffset > 0f || dragAmount > 0f) {
                                        change.consume()
                                        controlsVisible = false
                                        coroutineScope.launch { dismissAnimY.snapTo(newOffset) }
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (dismissAnimY.value > 160f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dismissAnimY.animateTo(
                                                targetValue = 1800f,
                                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                                            )
                                            onClose()
                                        } else {
                                            dismissAnimY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dismissAnimY.animateTo(0f)
                                    }
                                }
                            )
                        }
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isCurrentPhotoZoomed,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = viewerHazeState),
                    key = { mediaList.getOrNull(it)?.id ?: it }
                ) { page ->
                    val item = mediaList.getOrNull(page) ?: return@HorizontalPager
                    val isCurrent = page == pagerState.currentPage
                    if (item.isVideo) {
                        VideoPlayerView(
                            item = item,
                            isActivePage = isCurrent,
                            controlsVisible = controlsVisible,
                            onToggleControls = { controlsVisible = !controlsVisible }
                        )
                    } else {
                        ZoomableMediaView(
                            item = item,
                            isActivePage = isCurrent,
                            onSingleTap = { controlsVisible = !controlsVisible },
                            onZoomStateChanged = { zoomed ->
                                if (isCurrent) {
                                    isCurrentPhotoZoomed = zoomed
                                    if (zoomed) {
                                        controlsVisible = false
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Top Floating Frosted Glass Header (hidden while actively dragging to dismiss)
            AnimatedVisibility(
                visible = controlsVisible && currentOffsetY == 0f,
                enter = fadeIn(tween(220)) + slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(160)) + slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarPadding + 8.dp, start = 16.dp, end = 16.dp)
            ) {
                val pillShape = CircleShape
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, shape = pillShape, spotColor = Color.Black.copy(alpha = 0.35f))
                        .clip(pillShape)
                        .hazeEffect(state = viewerHazeState, style = FrostedGlassDefaults.photoViewerStyle())
                        .background(FrostedGlassDefaults.photoViewerBackground())
                        .border(FrostedGlassDefaults.photoViewerBorder(), shape = pillShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = currentItem.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatHeaderDate(currentItem.dateTaken),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }

                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Details",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Floating Frosted Glass Action Pill (hidden while actively dragging to dismiss)
            AnimatedVisibility(
                visible = controlsVisible && currentOffsetY == 0f,
                enter = fadeIn(tween(220)) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(160)) + slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 12.dp, end = 12.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val pillShape = CircleShape
                    Box(
                        modifier = Modifier
                            .shadow(14.dp, shape = pillShape, spotColor = Color.Black.copy(alpha = 0.40f))
                            .clip(pillShape)
                            .hazeEffect(state = viewerHazeState, style = FrostedGlassDefaults.photoViewerStyle())
                            .background(FrostedGlassDefaults.photoViewerBackground())
                            .border(FrostedGlassDefaults.photoViewerBorder(), shape = pillShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share
                            IconButton(
                                onClick = { shareMedia(context, currentItem) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }

                            // Edit (Photos only)
                            if (!currentItem.isVideo) {
                                IconButton(
                                    onClick = { showEditorSheet = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoFixHigh,
                                        contentDescription = "Edit photo",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Use As (System wallpaper, contact photo, profile photo)
                            IconButton(
                                onClick = { useMediaAs(context, currentItem) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = "Use as",
                                    tint = Color.White
                                )
                            }

                            // Favorite toggle (Instant optimistic feedback with vibrant red heart)
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val nextFav = !isCurrentFav
                                    localFavMap = localFavMap + (currentItem.id to nextFav)
                                    onToggleFavorite(currentItem)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrentFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (isCurrentFav) "Unfavourite" else "Favourite",
                                    tint = if (isCurrentFav) Color(0xFFFF4D4D) else Color.White
                                )
                            }

                            // Delete / Move to Bin
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
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
        }
    }

    // Confirmation dialog before putting into bin
    if (showDeleteConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Move to Bin?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This ${if (currentItem.isVideo) "video" else "photo"} will be moved to the Bin.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onMoveToBin(currentItem)
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
                androidx.compose.material3.OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Media Details Sheet
    if (showInfoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        MediaInfoSheet(
            item = currentItem,
            sheetState = sheetState,
            onDismissRequest = { showInfoSheet = false }
        )
    }

    // Fullscreen Photo Editor
    if (showEditorSheet) {
        PhotoEditorScreen(
            item = currentItem,
            onClose = { showEditorSheet = false },
            onSavedCopy = {
                onImageEdited()
            }
        )
    }
}

private fun useMediaAs(context: Context, item: MediaItem) {
    try {
        val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(item.uri, item.mimeType)
            putExtra("mimeType", item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Set picture as"))
    } catch (e: android.content.ActivityNotFoundException) {
        // Safe fallback when no application handles ACTION_ATTACH_DATA on device
    } catch (e: SecurityException) {
        // Safe fallback when activity launch permission is restricted
    }
}

private fun shareMedia(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Media"))
}

private fun formatHeaderDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun ZoomableMediaView(
    item: MediaItem,
    isActivePage: Boolean,
    onSingleTap: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val scaleAnim = remember(item.id) { Animatable(1f) }
    val offsetXAnim = remember(item.id) { Animatable(0f) }
    val offsetYAnim = remember(item.id) { Animatable(0f) }

    LaunchedEffect(isActivePage) {
        if (!isActivePage) {
            scaleAnim.snapTo(1f)
            offsetXAnim.snapTo(0f)
            offsetYAnim.snapTo(0f)
            onZoomStateChanged(false)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        fun maxPanX(s: Float) = ((s - 1f) * containerWidth / 2f).coerceAtLeast(0f)
        fun maxPanY(s: Float) = ((s - 1f) * containerHeight / 2f).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(item.id) {
                    detectTapGestures(
                        onTap = { onSingleTap() },
                        onDoubleTap = { tapOffset ->
                            coroutineScope.launch {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (scaleAnim.value > 1.05f) {
                                    onZoomStateChanged(false)
                                    launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                    launch { offsetXAnim.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                    launch { offsetYAnim.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                } else {
                                    val targetScale = 2.5f
                                    onZoomStateChanged(true)
                                    val maxX = maxPanX(targetScale)
                                    val maxY = maxPanY(targetScale)
                                    val center = Offset(containerWidth / 2f, containerHeight / 2f)
                                    val targetX = ((center.x - tapOffset.x) * (targetScale - 1f)).coerceIn(-maxX, maxX)
                                    val targetY = ((center.y - tapOffset.y) * (targetScale - 1f)).coerceIn(-maxY, maxY)
                                    launch { scaleAnim.animateTo(targetScale, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                    launch { offsetXAnim.animateTo(targetX, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                    launch { offsetYAnim.animateTo(targetY, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                }
                            }
                        }
                    )
                }
                .pointerInput(item.id) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            val pointerCount = activePointers.size

                            if (pointerCount >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()

                                val currentScale = scaleAnim.value
                                val newScale = (currentScale * zoom).coerceIn(0.75f, 6.0f)

                                val currentX = offsetXAnim.value
                                val currentY = offsetYAnim.value

                                val maxX = maxPanX(newScale)
                                val maxY = maxPanY(newScale)

                                val zoomRatio = if (currentScale > 0.001f) newScale / currentScale else 1f
                                val shiftX = (1f - zoomRatio) * (centroid.x - containerWidth / 2f - currentX)
                                val shiftY = (1f - zoomRatio) * (centroid.y - containerHeight / 2f - currentY)

                                val newX = (currentX + pan.x + shiftX).coerceIn(-maxX * 1.3f, maxX * 1.3f)
                                val newY = (currentY + pan.y + shiftY).coerceIn(-maxY * 1.3f, maxY * 1.3f)

                                event.changes.forEach { change ->
                                    if (change.positionChanged()) change.consume()
                                }

                                coroutineScope.launch {
                                    scaleAnim.snapTo(newScale)
                                    offsetXAnim.snapTo(newX)
                                    offsetYAnim.snapTo(newY)
                                }

                                val isZoomedNow = newScale > 1.05f
                                onZoomStateChanged(isZoomedNow)
                            } else if (pointerCount == 1 && scaleAnim.value > 1.05f) {
                                val change = activePointers.firstOrNull()
                                if (change != null) {
                                    val pan = change.positionChange()
                                    if (pan != Offset.Zero) {
                                        change.consume()
                                        val maxX = maxPanX(scaleAnim.value)
                                        val maxY = maxPanY(scaleAnim.value)
                                        val newX = (offsetXAnim.value + pan.x).coerceIn(-maxX, maxX)
                                        val newY = (offsetYAnim.value + pan.y).coerceIn(-maxY, maxY)
                                        coroutineScope.launch {
                                            offsetXAnim.snapTo(newX)
                                            offsetYAnim.snapTo(newY)
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        val finalScale = scaleAnim.value
                        if (finalScale < 1.05f) {
                            onZoomStateChanged(false)
                            coroutineScope.launch {
                                launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetXAnim.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetYAnim.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                            }
                        } else {
                            val targetScale = finalScale.coerceIn(1f, 5f)
                            val maxX = maxPanX(targetScale)
                            val maxY = maxPanY(targetScale)
                            val clampedX = offsetXAnim.value.coerceIn(-maxX, maxX)
                            val clampedY = offsetYAnim.value.coerceIn(-maxY, maxY)

                            if (targetScale != finalScale || clampedX != offsetXAnim.value || clampedY != offsetYAnim.value) {
                                coroutineScope.launch {
                                    if (targetScale != finalScale) {
                                        launch { scaleAnim.animateTo(targetScale, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                    }
                                    if (clampedX != offsetXAnim.value) {
                                        launch { offsetXAnim.animateTo(clampedX, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                    }
                                    if (clampedY != offsetYAnim.value) {
                                        launch { offsetYAnim.animateTo(clampedY, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .crossfade(true)
                    .precision(coil3.size.Precision.INEXACT)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        translationX = offsetXAnim.value
                        translationY = offsetYAnim.value
                    }
            )
        }
    }
}

@Composable
private fun rememberTabletopMode(): Boolean {
    val context = LocalContext.current
    val activity = context as? Activity ?: return false
    var isTabletop by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collect { layoutInfo ->
                val foldingFeature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                isTabletop = foldingFeature != null &&
                        foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                        foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
            }
    }
    return isTabletop
}

@Composable
private fun TabletopControlPanel(
    item: MediaItem,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onUseAs: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatHeaderDate(item.dateTaken),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onShowInfo) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare) {
                    Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                }
                if (!item.isVideo) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Rounded.AutoFixHigh, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                IconButton(onClick = onUseAs) {
                    Icon(imageVector = Icons.Rounded.Wallpaper, contentDescription = "Use as", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF4D4D) else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Flex Mode • Lower Control Screen",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (item.isVideo) "Video" else "Photo"} • ${if (item.resolutionText.isNotBlank()) item.resolutionText + " • " else ""}${item.formattedSize}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Folder: ${item.bucketName.ifBlank { "Internal Storage" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


