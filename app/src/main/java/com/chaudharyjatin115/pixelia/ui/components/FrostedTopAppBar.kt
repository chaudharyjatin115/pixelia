package com.chaudharyjatin115.pixelia.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

/**
 * Legacy wrapper for [ExpressiveTopAppBar]. Blur is permanently removed in favor of
 * clean, battery-efficient Material You Expressive theming in both Light and Dark modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    canScroll: Boolean = true,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    totalCount: Int = 0,
    onBackClick: (() -> Unit)? = null,
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    ExpressiveTopAppBar(
        title = title,
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        canScroll = canScroll,
        isSelectionMode = isSelectionMode,
        selectedCount = selectedCount,
        totalCount = totalCount,
        onBackClick = onBackClick,
        onClearSelection = onClearSelection,
        onSelectAll = onSelectAll,
        actions = actions
    )
}
