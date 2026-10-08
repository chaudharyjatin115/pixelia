package com.chaudharyjatin115.pixelia.ui.screens.folders

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.chaudharyjatin115.pixelia.domain.model.MediaFolder
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import com.chaudharyjatin115.pixelia.ui.components.EmptyState
import com.chaudharyjatin115.pixelia.ui.components.ExpressiveTopAppBar
import com.chaudharyjatin115.pixelia.ui.components.MediaThumbnail
import com.chaudharyjatin115.pixelia.ui.screens.mediaIdRange
import java.util.Locale

enum class FolderSortOption(val displayName: String) {
    RECENT("Recent"),
    NAME_AZ("Name (A–Z)"),
    NAME_ZA("Name (Z–A)"),
    MODIFIED("Modified")
}

@Composable
fun FoldersScreen(
    folders: List<MediaFolder>,
    onFolderClick: (MediaFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    FoldersGridContent(
        folders = folders,
        selectedBucketId = null,
        onFolderClick = onFolderClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoldersGridContent(
    folders: List<MediaFolder>,
    selectedBucketId: String?,
    onFolderClick: (MediaFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    var sortOption by rememberSaveable { mutableStateOf(FolderSortOption.RECENT) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val sortedFolders = remember(folders, sortOption) {
        when (sortOption) {
            FolderSortOption.RECENT -> folders.sortedWith(
                compareByDescending<MediaFolder> { it.bucketId == "camera_default_unified" }
                    .thenByDescending { it.latestDateTaken }
                    .thenByDescending { it.mediaCount }
            )
            FolderSortOption.NAME_AZ -> folders.sortedBy { it.bucketName.lowercase(Locale.getDefault()) }
            FolderSortOption.NAME_ZA -> folders.sortedByDescending { it.bucketName.lowercase(Locale.getDefault()) }
            FolderSortOption.MODIFIED -> folders.sortedByDescending { it.latestDateModified }
        }
    }

    val canScrollContent by remember(sortedFolders) {
        derivedStateOf {
            sortedFolders.isNotEmpty() && (gridState.canScrollForward || gridState.canScrollBackward)
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

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val topContentPadding = statusBarHeight + (if (isLandscape) 52.dp else 64.dp) + 4.dp
    val bottomContentPadding = navBarHeight + (if (isLandscape) 88.dp else 144.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        if (sortedFolders.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Folder,
                title = "No folders found",
                description = "Folders containing pictures and videos will appear here automatically."
            )
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = if (selectedBucketId != null) 120.dp else 140.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = topContentPadding,
                    bottom = bottomContentPadding
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = sortedFolders,
                    key = { it.bucketId },
                    contentType = { "folder_item" }
                ) { folder ->
                    val isSelected = folder.bucketId == selectedBucketId
                    FolderGridItem(
                        folder = folder,
                        isSelected = isSelected,
                        onClick = { onFolderClick(folder) }
                    )
                }
            }
        }

        ExpressiveTopAppBar(
            title = "Folders",
            scrollBehavior = scrollBehavior,
            canScroll = canScrollContent,
            actions = {
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort folders",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        FolderSortOption.entries.forEach { option ->
                            val isSelectedOption = option == sortOption
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontWeight = if (isSelectedOption) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelectedOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (isSelectedOption) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    sortOption = option
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun FolderGridItem(
    folder: MediaFolder,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageShape = RoundedCornerShape(20.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "folder_item_scale"
    )

    val itemBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(itemBg)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
                onClick = onClick
            )
            .padding(all = if (isSelected) 4.dp else 0.dp)
            .padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (folder.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(folder.coverUri)
                        .size(400, 400)
                        .crossfade(true)
                        .build(),
                    contentDescription = folder.bucketName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveFolderIcon(folder.bucketName),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveFolderIcon(folder.bucketName),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = folder.bucketName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val countText = if (folder.mediaCount == 1) "1 item" else "${folder.mediaCount} items"
            val subtitleText = if (folder.totalSizeBytes > 0) {
                "$countText • ${folder.formattedSize}"
            } else {
                countText
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: MediaFolder,
    items: List<MediaItem>,
    isSelectionMode: Boolean = false,
    selectedMediaIds: Set<Long> = emptySet(),
    onBackClick: () -> Unit,
    onPhotoClick: (MediaItem, List<MediaItem>) -> Unit,
    onItemClick: (MediaItem) -> Unit = {},
    onItemLongClick: (MediaItem) -> Unit = {},
    onUpdateSelection: (Set<Long>) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var sortOption by rememberSaveable { mutableStateOf(FolderSortOption.RECENT) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val sortedItems = remember(items, sortOption) {
        when (sortOption) {
            FolderSortOption.RECENT -> items.sortedWith(
                compareByDescending<MediaItem> { if (it.dateTaken > 0) it.dateTaken else it.dateModified }
                    .thenByDescending { it.id }
            )
            FolderSortOption.NAME_AZ -> items.sortedWith(
                compareBy<MediaItem> { it.name.lowercase(Locale.getDefault()) }
                    .thenByDescending { it.id }
            )
            FolderSortOption.NAME_ZA -> items.sortedWith(
                compareByDescending<MediaItem> { it.name.lowercase(Locale.getDefault()) }
                    .thenByDescending { it.id }
            )
            FolderSortOption.MODIFIED -> items.sortedWith(
                compareByDescending<MediaItem> { it.dateModified }
                    .thenByDescending { it.id }
            )
        }
    }

    val canScrollContent by remember(sortedItems, isSelectionMode) {
        derivedStateOf {
            !isSelectionMode && sortedItems.isNotEmpty() && (gridState.canScrollForward || gridState.canScrollBackward)
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

    val totalFolderItems = sortedItems.size
    var isDraggingScrubber by remember { mutableStateOf(false) }
    var scrubberDragFraction by remember { mutableFloatStateOf(0f) }
    var isScrollingActive by remember { mutableStateOf(false) }

    LaunchedEffect(gridState.isScrollInProgress, isDraggingScrubber) {
        if (gridState.isScrollInProgress || isDraggingScrubber) {
            isScrollingActive = true
        } else {
            delay(1200L)
            isScrollingActive = false
        }
    }

    val activeDateHeader by remember(sortedItems) {
        derivedStateOf {
            val idx = if (isDraggingScrubber) {
                (scrubberDragFraction * (totalFolderItems - 1).coerceAtLeast(0)).toInt()
            } else {
                gridState.firstVisibleItemIndex
            }
            val item = sortedItems.getOrNull(idx)
            if (item != null) {
                val ts = if (item.dateTaken > 0) item.dateTaken else item.dateModified
                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                sdf.format(Date(ts))
            } else ""
        }
    }

    val thumbFraction by remember {
        derivedStateOf {
            if (isDraggingScrubber) {
                scrubberDragFraction
            } else if (totalFolderItems > 0) {
                (gridState.firstVisibleItemIndex.toFloat() / totalFolderItems.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val currentSortedItems by rememberUpdatedState(sortedItems)
    val sortedItemIds = remember(sortedItems) { sortedItems.map { it.id } }
    val currentSortedItemIds by rememberUpdatedState(sortedItemIds)
    val currentSelectedMediaIds by rememberUpdatedState(selectedMediaIds)
    val currentOnUpdateSelection by rememberUpdatedState(onUpdateSelection)

    fun findItemAtOffset(offset: Offset): MediaItem? {
        val visibleItems = gridState.layoutInfo.visibleItemsInfo
        for (itemInfo in visibleItems) {
            val rect = IntRect(
                left = itemInfo.offset.x,
                top = itemInfo.offset.y,
                right = itemInfo.offset.x + itemInfo.size.width,
                bottom = itemInfo.offset.y + itemInfo.size.height
            )
            if (rect.contains(IntOffset(offset.x.toInt(), offset.y.toInt()))) {
                val key = itemInfo.key as? Long
                if (key != null) {
                    return currentSortedItems.firstOrNull { it.id == key }
                }
            }
        }
        return null
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val topContentPadding = statusBarHeight + (if (isLandscape) 52.dp else 64.dp) + 4.dp
    val bottomContentPadding = navBarHeight + (if (isLandscape) 88.dp else 144.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pointerInput(gridState, isSelectionMode) {
                if (isSelectionMode) {
                    var dragSelectStartId: Long? = null
                    var initialSelectedIds = emptySet<Long>()
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val startItem = findItemAtOffset(offset)
                            if (startItem != null) {
                                dragSelectStartId = startItem.id
                                initialSelectedIds = currentSelectedMediaIds + startItem.id
                                currentOnUpdateSelection(initialSelectedIds)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val startId = dragSelectStartId ?: return@detectDragGesturesAfterLongPress
                            val currentItem = findItemAtOffset(change.position) ?: return@detectDragGesturesAfterLongPress
                            val rangeIds = mediaIdRange(currentSortedItemIds, startId, currentItem.id)
                            if (rangeIds.isNotEmpty()) {
                                currentOnUpdateSelection(initialSelectedIds + rangeIds)
                            }
                        }
                    )
                }
            }
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 105.dp),
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
            items(
                items = sortedItems,
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
                            onPhotoClick(item, sortedItems)
                        }
                    },
                    onLongClick = if (isSelectionMode) null else ({ onItemLongClick(item) }),
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected
                )
            }
        }

        AnimatedVisibility(
            visible = (isScrollingActive || isDraggingScrubber) && totalFolderItems > 12,
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

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(36.dp)
                        .pointerInput(totalFolderItems) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    isDraggingScrubber = true
                                    val frac = if (maxOffset > 0f) {
                                        ((offset.y - thumbHeightPx / 2f) / maxOffset).coerceIn(0f, 1f)
                                    } else 0f
                                    scrubberDragFraction = frac
                                    val targetIdx = (frac * (totalFolderItems - 1).coerceAtLeast(0)).toInt()
                                    coroutineScope.launch { gridState.scrollToItem(targetIdx) }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newY = (scrubberDragFraction * maxOffset + dragAmount).coerceIn(0f, maxOffset)
                                    val frac = if (maxOffset > 0f) newY / maxOffset else 0f
                                    scrubberDragFraction = frac
                                    val targetIdx = (frac * (totalFolderItems - 1).coerceAtLeast(0)).toInt()
                                    coroutineScope.launch { gridState.scrollToItem(targetIdx) }
                                },
                                onDragEnd = { isDraggingScrubber = false },
                                onDragCancel = { isDraggingScrubber = false }
                            )
                        }
                )

                if (activeDateHeader.isNotEmpty()) {
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), shape = CircleShape)
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
                            text = activeDateHeader,
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

        ExpressiveTopAppBar(
            title = folder.bucketName,
            subtitle = if (sortedItems.size == 1) "1 item" else "${sortedItems.size} items",
            scrollBehavior = scrollBehavior,
            canScroll = canScrollContent,
            isSelectionMode = isSelectionMode,
            selectedCount = selectedMediaIds.size,
            totalCount = sortedItems.size,
            onBackClick = onBackClick,
            onClearSelection = onClearSelection,
            onSelectAll = onSelectAll,
            actions = {
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort folder items",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        FolderSortOption.entries.forEach { option ->
                            val isSelectedOption = option == sortOption
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontWeight = if (isSelectedOption) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelectedOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (isSelectedOption) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    sortOption = option
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private fun resolveFolderIcon(bucketName: String): ImageVector {
    val name = bucketName.lowercase(Locale.getDefault())
    return when {
        name.contains("camera") || name.contains("dcim") -> Icons.Rounded.CameraAlt
        name.contains("screenshot") -> Icons.Rounded.Screenshot
        name.contains("video") || name.contains("movie") -> Icons.Rounded.Videocam
        name.contains("download") || name.contains("picture") -> Icons.Rounded.Image
        else -> Icons.Rounded.Folder
    }
}
