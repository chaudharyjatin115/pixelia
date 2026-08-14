package com.chaudharyjatin115.pixelia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class GalleryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    PHOTOS(
        route = "photos",
        label = "Photos",
        icon = Icons.Rounded.PhotoLibrary
    ),
    FOLDERS(
        route = "folders",
        label = "Folders",
        icon = Icons.Rounded.Folder
    ),
    FAVOURITES(
        route = "favourites",
        label = "Favourites",
        icon = Icons.Rounded.Favorite
    ),
    BIN(
        route = "bin",
        label = "Bin",
        icon = Icons.Rounded.Delete
    )
}
