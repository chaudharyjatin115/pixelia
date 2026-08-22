package com.chaudharyjatin115.pixelia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chrisbanes.haze.HazeState

enum class FolderAction(
    val label: String,
    val icon: ImageVector
) {
    SHARE("Share", Icons.Rounded.Share),
    DELETE("Delete", Icons.Rounded.Delete),
    COPY("Copy", Icons.Rounded.ContentCopy),
    MOVE("Move", Icons.AutoMirrored.Rounded.DriveFileMove)
}

@Composable
fun FolderActionBar(
    activeAction: FolderAction?,
    onActionClick: (FolderAction) -> Unit,
    modifier: Modifier = Modifier,
    actions: List<FolderAction> = FolderAction.entries,
    hazeState: HazeState? = null
) {
    val currentSelected = activeAction ?: actions.first()
    FloatingPillBar(
        items = actions,
        selectedItem = currentSelected,
        onItemSelected = onActionClick,
        getItemLabel = { it.label },
        getItemIcon = { it.icon },
        modifier = modifier,
        hazeState = hazeState,
        isTabRole = false
    )
}
