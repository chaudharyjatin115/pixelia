package com.chaudharyjatin115.pixelia.ui.screens.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import com.chaudharyjatin115.pixelia.ui.components.EmptyState
import com.chaudharyjatin115.pixelia.ui.components.ExpressiveTopAppBar
import com.chaudharyjatin115.pixelia.ui.components.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    favourites: List<MediaItem>,
    onPhotoClick: (MediaItem, List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val gridState = rememberLazyGridState()
    val canScrollContent by remember(favourites) {
        derivedStateOf {
            favourites.isNotEmpty() && (gridState.canScrollForward || gridState.canScrollBackward)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(hazeModifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        if (favourites.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.FavoriteBorder,
                title = "No favourites yet",
                description = "Photos and videos you favourite will appear here."
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
                    items = favourites,
                    key = { it.id },
                    contentType = { "media_thumbnail" }
                ) { item ->
                    MediaThumbnail(
                        item = item,
                        onClick = { onPhotoClick(item, favourites) }
                    )
                }
            }
        }

        ExpressiveTopAppBar(
            title = "Favourites",
            scrollBehavior = scrollBehavior,
            canScroll = canScrollContent,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
