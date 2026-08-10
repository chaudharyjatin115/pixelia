package com.chaudharyjatin115.pixelia.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Material You Expressive Top App Bar for Pixelia Gallery.
 *
 * Implements Android 16 / Material 3 Expressive principles:
 * - Pure solid / opaque background with ZERO blur in both Light and Dark modes.
 * - Dynamic Material You tonal container elevation (transitions from `surface` to `surfaceContainer` on scroll).
 * - Smooth collapse / expand when scrolling nested content (via [TopAppBarScrollBehavior]).
 * - Conditionally collapses ONLY when content is scrollable ([canScroll] is true).
 * - Expressive selection mode styling (elevated container, bold count, M3 primary actions).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    canScroll: Boolean = true,
    isScrolled: Boolean = false,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    totalCount: Int = 0,
    onBackClick: (() -> Unit)? = null,
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val barContentHeight = if (isLandscape) 52.dp else 64.dp
    val barContentHeightPx = with(LocalDensity.current) { barContentHeight.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val allowCollapsing = canScroll && !isSelectionMode

    SideEffect {
        if (scrollBehavior != null) {
            if (!allowCollapsing && scrollBehavior.state.heightOffset != 0f) {
                scrollBehavior.state.heightOffset = 0f
            }
            if (scrollBehavior.state.heightOffsetLimit != -barContentHeightPx) {
                scrollBehavior.state.heightOffsetLimit = -barContentHeightPx
            }
        }
    }

    LaunchedEffect(allowCollapsing) {
        if (!allowCollapsing && scrollBehavior != null) {
            scrollBehavior.state.heightOffset = 0f
        }
    }

    val heightOffset = if (allowCollapsing) (scrollBehavior?.state?.heightOffset ?: 0f) else 0f
    val hasScrolled = isScrolled || (allowCollapsing && scrollBehavior?.let {
        it.state.heightOffset < -1f || it.state.overlappedFraction > 0.01f
    } == true)

    val targetContainerColor = when {
        isSelectionMode -> MaterialTheme.colorScheme.surfaceContainerHigh
        hasScrolled -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "expressive_top_bar_container_color"
    )

    // Collapse content height from barContentHeight down to 0
    val currentContentHeightPx = if (allowCollapsing) {
        (barContentHeightPx + heightOffset).coerceIn(0f, barContentHeightPx)
    } else {
        barContentHeightPx
    }
    val currentContentHeightDp = with(LocalDensity.current) { currentContentHeightPx.toDp() }

    // Direct vertical drag support on the top bar itself (only if content is scrollable)
    val topBarDragModifier = if (scrollBehavior != null && allowCollapsing) {
        Modifier.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                scrollBehavior.state.heightOffset =
                    (scrollBehavior.state.heightOffset + delta).coerceIn(-barContentHeightPx, 0f)
            },
            onDragStopped = { velocity ->
                val current = scrollBehavior.state.heightOffset
                val target = if (velocity < -300f || (velocity <= 300f && current < -barContentHeightPx / 2)) {
                    -barContentHeightPx
                } else {
                    0f
                }
                coroutineScope.launch {
                    Animatable(scrollBehavior.state.heightOffset).animateTo(
                        targetValue = target,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) {
                        scrollBehavior.state.heightOffset = value
                    }
                }
            }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .then(topBarDragModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (!allowCollapsing) barContentHeight else currentContentHeightDp)
                    .clipToBounds()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barContentHeight)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = if (!allowCollapsing) 0 else ((currentContentHeightPx - barContentHeightPx) / 2).roundToInt()
                            )
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isSelectionMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onClearSelection) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Exit selection",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$selectedCount selected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(onClick = onSelectAll) {
                            Text(
                                text = if (selectedCount > 0 && selectedCount == totalCount) "Deselect all" else "Select all",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (onBackClick != null) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = if (onBackClick == null) 8.dp else 0.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!subtitle.isNullOrBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions
                        )
                    }
                }
            }

            // Hairline M3 Divider (fades in on scroll or selection)
            val dividerAlpha by animateFloatAsState(
                targetValue = if (hasScrolled || isSelectionMode) 0.35f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "top_bar_divider_alpha"
            )

            if (dividerAlpha > 0f) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = dividerAlpha),
                    thickness = 0.5.dp
                )
            }
        }
    }
}
