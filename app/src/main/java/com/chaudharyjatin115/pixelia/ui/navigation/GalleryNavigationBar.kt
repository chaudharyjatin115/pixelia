package com.chaudharyjatin115.pixelia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

@Composable
fun GalleryNavigationBar(
    selectedDestination: GalleryDestination,
    onDestinationSelected: (GalleryDestination) -> Unit,
    modifier: Modifier = Modifier,
    destinations: List<GalleryDestination> = GalleryDestination.entries,
    hazeState: HazeState? = null
) {
    FloatingPillBar(
        items = destinations,
        selectedItem = selectedDestination,
        onItemSelected = onDestinationSelected,
        getItemLabel = { it.label },
        getItemIcon = { it.icon },
        modifier = modifier,
        hazeState = hazeState,
        isTabRole = true
    )
}
