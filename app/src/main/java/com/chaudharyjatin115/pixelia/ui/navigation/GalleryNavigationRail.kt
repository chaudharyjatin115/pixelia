package com.chaudharyjatin115.pixelia.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Adaptive navigation rail for wider screens (tablets, foldables, landscape)
 * maintaining the same expressive pill aesthetic and selective icon visibility.
 */
@Composable
fun GalleryNavigationRail(
    selectedDestination: GalleryDestination,
    onDestinationSelected: (GalleryDestination) -> Unit,
    modifier: Modifier = Modifier,
    destinations: List<GalleryDestination> = GalleryDestination.entries
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, top = 24.dp, bottom = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val containerShape = RoundedCornerShape(32.dp)

        Box(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = containerShape,
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(containerShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    shape = containerShape
                )
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                destinations.forEach { destination ->
                    val isSelected = destination == selectedDestination

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "rail_bg_color"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(180),
                        label = "rail_content_color"
                    )

                    val itemShape = CircleShape

                    Box(
                        modifier = Modifier
                            .clip(itemShape)
                            .background(pillBgColor, itemShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                role = Role.Tab,
                                onClick = { onDestinationSelected(destination) }
                            )
                            .semantics {
                                this.role = Role.Tab
                                this.selected = isSelected
                                this.contentDescription = "${destination.label} tab, ${if (isSelected) "selected" else "unselected"}"
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(tween(180)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(tween(140)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = contentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }

                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
