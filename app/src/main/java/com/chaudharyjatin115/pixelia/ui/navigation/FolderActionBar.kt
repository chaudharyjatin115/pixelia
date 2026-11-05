package com.chaudharyjatin115.pixelia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

enum class FolderAction(
    val label: String,
    val icon: ImageVector
) {
    COPY("Copy", Icons.Rounded.ContentCopy),
    MOVE("Move", Icons.AutoMirrored.Rounded.DriveFileMove),
    SHARE("Share", Icons.Rounded.Share),
    DELETE("Delete", Icons.Rounded.Delete)
}

@Composable
fun FolderActionBar(
    activeAction: FolderAction?,
    onActionClick: (FolderAction) -> Unit,
    modifier: Modifier = Modifier,
    actions: List<FolderAction> = FolderAction.entries,
    contentAlignment: Alignment = Alignment.Center
) {
    val currentSelected = activeAction ?: actions.first()
    FloatingPillBar(
        items = actions,
        selectedItem = currentSelected,
        onItemSelected = onActionClick,
        getItemLabel = { it.label },
        getItemIcon = { it.icon },
        modifier = modifier,
        contentAlignment = contentAlignment,
        isTabRole = false
    )
}
