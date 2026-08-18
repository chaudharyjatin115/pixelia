package com.chaudharyjatin115.pixelia.ui.screens.photos

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.chaudharyjatin115.pixelia.domain.model.DateGroupedMedia
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import com.chaudharyjatin115.pixelia.ui.components.EmptyState
import com.chaudharyjatin115.pixelia.ui.components.ExpressiveTopAppBar
import com.chaudharyjatin115.pixelia.ui.components.MediaThumbnail
import com.chaudharyjatin115.pixelia.ui.components.PartialAccessBanner
import com.chaudharyjatin115.pixelia.ui.theme.FrostedGlassDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    groupedMedia: List<DateGroupedMedia>,
    allMedia: List<MediaItem>,
    isLoading: Boolean,
    isPartialAccess: Boolean,
    isSelectionMode: Boolean = false,
    selectedMediaIds: Set<Long> = emptySet(),
    onPhotoClick: (MediaItem, List<MediaItem>) -> Unit,
    onItemClick: (MediaItem) -> Unit = {},
    onItemLongClick: (MediaItem) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onRefresh: () -> Unit,
    onManagePermissions: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val gridState = rememberLazyGridState()
    val canScrollContent by remember(groupedMedia, isSelectionMode) {
        derivedStateOf {
            !isSelectionMode && groupedMedia.isNotEmpty() && (gridState.canScrollForward || gridState.canScrollBackward)
        }
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = { canScrollContent }
    )

    LaunchedEffect(canScrollContent) {
        if (!canScrollContent) {
            scrollBehavior.state.heightOffset = 0f
        }
    }
    val hazeModifier = if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val topContentPadding = statusBarHeight + (if (isLandscape) 52.dp else 64.dp) + 4.dp
    val bottomContentPadding = navBarHeight + (if (isLandscape) 88.dp else 144.dp)

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Dynamic grid density (Pinch-to-zoom & toggle button)
    val minColumns = if (isLandscape) 3 else 2
    val maxColumns = if (isLandscape) 6 else 4
    var columnCount by remember(isLandscape) { mutableIntStateOf(if (isLandscape) 5 else 3) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    // Fast scrubber calculation
    val totalGridItems = remember(groupedMedia, isPartialAccess) {
        (if (isPartialAccess) 1 else 0) + groupedMedia.sumOf { 1 + it.items.size }
    }

    fun getHeaderForIndex(index: Int): String {
        var curr = if (isPartialAccess) 1 else 0
        for (group in groupedMedia) {
            val count = 1 + group.items.size
            if (index < curr + count) {
                return group.header
            }
            curr += count
        }
        return groupedMedia.lastOrNull()?.header ?: ""
    }

    var isDraggingScrubber by remember { mutableStateOf(false) }
    var scrubberDragFraction by remember { mutableFloatStateOf(0f) }
    var isScrollingActive by remember { mutableStateOf(false) }

    // Visibility timeout for scrubber bubble
    LaunchedEffect(gridState.isScrollInProgress, isDraggingScrubber) {
        if (gridState.isScrollInProgress || isDraggingScrubber) {
            isScrollingActive = true
        } else {
            delay(1200L)
            isScrollingActive = false
        }
    }

    val activeHeader by remember {
        derivedStateOf {
            val idx = if (isDraggingScrubber) {
                (scrubberDragFraction * (totalGridItems - 1).coerceAtLeast(0)).toInt()
            } else {
                gridState.firstVisibleItemIndex
            }
            getHeaderForIndex(idx)
        }
    }

    // Tactile haptic feedback when crossing date boundaries during drag
    LaunchedEffect(activeHeader) {
        if (isDraggingScrubber && activeHeader.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val thumbFraction by remember {
        derivedStateOf {
            if (isDraggingScrubber) {
                scrubberDragFraction
            } else if (totalGridItems > 0) {
                (gridState.firstVisibleItemIndex.toFloat() / totalGridItems.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(hazeModifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        if (groupedMedia.isEmpty() && !isLoading) {
            EmptyState(
                icon = Icons.Rounded.PhotoLibrary,
                title = "No photos or videos",
                description = "Photos and videos on your device will appear here once captured or downloaded.",
                actionLabel = "Refresh",
                onActionClick = onRefresh
            )
        } else {
            // Main grid with pinch-to-zoom gesture listener
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(columnCount, minColumns, maxColumns) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomScale *= zoom
                            if (zoomScale > 1.28f) {
                                if (columnCount > minColumns) {
                                    columnCount--
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                zoomScale = 1f
                            } else if (zoomScale < 0.75f) {
                                if (columnCount < maxColumns) {
                                    columnCount++
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                zoomScale = 1f
                            }
                        }
                    }
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columnCount),
                    contentPadding = PaddingValues(
                        start = 10.dp,
                        end = 10.dp,
                        top = topContentPadding,
                        bottom = bottomContentPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isPartialAccess) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PartialAccessBanner(
                                onManageAccess = onManagePermissions,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    groupedMedia.forEach { group ->
                        item(
                            key = "header_${group.dateKey}",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = { "section_header" }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 6.dp, start = 4.dp)
                            ) {
                                Text(
                                    text = group.header,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        items(
                            items = group.items,
                            key = { it.id },
                            contentType = { "media_thumbnail" }
                        ) { item ->
                            val isSelected = selectedMediaIds.contains(item.id)
                            MediaThumbnail(
                                item = item,
                                onClick = {
                                    if (isSelectionMode) {
                                        onItemClick(item)
                                    } else {
                                        onPhotoClick(item, allMedia)
                                    }
                                },
                                onLongClick = {
                                    onItemLongClick(item)
                                },
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected
                            )
                        }
                    }
                }
            }

            // Interactive Date Scroller / Timeline scrubber on the right edge
            AnimatedVisibility(
                visible = (isScrollingActive || isDraggingScrubber) && totalGridItems > 15,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(400)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(
                        top = topContentPadding + 16.dp,
                        bottom = bottomContentPadding + 16.dp,
                        end = 4.dp
                    )
                    .width(220.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val trackHeightPx = constraints.maxHeight.toFloat()
                    val thumbHeightDp = 44.dp
                    val thumbHeightPx = with(LocalDensity.current) { thumbHeightDp.toPx() }
                    val maxOffset = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
                    val currentThumbY = thumbFraction * maxOffset

                    // Touch target for scrubbing
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(36.dp)
                            .pointerInput(totalGridItems) {
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        isDraggingScrubber = true
                                        val frac = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                        scrubberDragFraction = frac
                                        val targetIdx = (frac * (totalGridItems - 1).coerceAtLeast(0)).toInt()
                                        coroutineScope.launch { gridState.scrollToItem(targetIdx) }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val newY = (scrubberDragFraction * trackHeightPx + dragAmount).coerceIn(0f, trackHeightPx)
                                        val frac = (newY / trackHeightPx).coerceIn(0f, 1f)
                                        scrubberDragFraction = frac
                                        val targetIdx = (frac * (totalGridItems - 1).coerceAtLeast(0)).toInt()
                                        coroutineScope.launch { gridState.scrollToItem(targetIdx) }
                                    },
                                    onDragEnd = { isDraggingScrubber = false },
                                    onDragCancel = { isDraggingScrubber = false }
                                )
                            }
                    )

                    // Floating Date Bubble
                    if (activeHeader.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = -16.dp.roundToPx(),
                                        y = (currentThumbY - 6.dp.roundToPx()).toInt()
                                    )
                                }
                                .align(Alignment.TopEnd)
                                .shadow(8.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f))
                                .border(FrostedGlassDefaults.border(), shape = CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeHeader,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = 0, y = currentThumbY.toInt()) }
                            .align(Alignment.TopEnd)
                            .size(width = 5.dp, height = thumbHeightDp)
                            .shadow(4.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(
                                if (isDraggingScrubber) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                            )
                    )
                }
            }
        }

        ExpressiveTopAppBar(
            title = "Photos",
            scrollBehavior = scrollBehavior,
            canScroll = canScrollContent,
            isSelectionMode = isSelectionMode,
            selectedCount = selectedMediaIds.size,
            totalCount = allMedia.size,
            onClearSelection = onClearSelection,
            onSelectAll = onSelectAll,
            actions = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        columnCount = if (columnCount >= maxColumns) minColumns else columnCount + 1
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Change grid size",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh photos",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
