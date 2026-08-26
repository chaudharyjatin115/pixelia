package com.chaudharyjatin115.pixelia.ui.screens.bin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import com.chaudharyjatin115.pixelia.ui.components.EmptyState
import com.chaudharyjatin115.pixelia.ui.components.ExpressiveTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinScreen(
    binMedia: List<MediaItem>,
    onRestore: (MediaItem) -> Unit,
    onPermanentDelete: (MediaItem) -> Unit,
    onEmptyBin: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val gridState = rememberLazyGridState()
    val canScrollContent by remember(binMedia) {
        derivedStateOf {
            binMedia.isNotEmpty() && (gridState.canScrollForward || gridState.canScrollBackward)
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val topContentPadding = statusBarHeight + (if (isLandscape) 52.dp else 64.dp) + 4.dp
    val bottomContentPadding = navBarHeight + (if (isLandscape) 88.dp else 144.dp)

    var selectedItemForAction by remember { mutableStateOf<MediaItem?>(null) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(hazeModifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        if (binMedia.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.DeleteOutline,
                title = "Bin is empty",
                description = "Deleted photos and videos will appear here."
            )
        } else {
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
                    items = binMedia,
                    key = { it.id },
                    contentType = { "media_thumbnail" }
                ) { item ->
                    BinThumbnail(
                        item = item,
                        onClick = { selectedItemForAction = item }
                    )
                }
            }
        }

        ExpressiveTopAppBar(
            title = "Bin",
            scrollBehavior = scrollBehavior,
            canScroll = canScrollContent,
            actions = {
                if (binMedia.isNotEmpty()) {
                    TextButton(onClick = { showEmptyBinDialog = true }) {
                        Text(
                            text = "Empty Bin",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    selectedItemForAction?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedItemForAction = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.trashedTime > 0) {
                    Text(
                        text = "Deleted on ${formatBinDate(item.trashedTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val target = selectedItemForAction
                            selectedItemForAction = null
                            target?.let { onRestore(it) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore")
                    }

                    Button(
                        onClick = {
                            val target = selectedItemForAction
                            selectedItemForAction = null
                            target?.let { onPermanentDelete(it) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyBinDialog = false },
            title = {
                Text(
                    text = "Empty Bin?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("All ${binMedia.size} items in the bin will be permanently deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyBinDialog = false
                        onEmptyBin()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Empty Bin")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEmptyBinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BinThumbnail(
    item: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val itemShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(itemShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(360, 360)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatBinDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}
