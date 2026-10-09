package com.chaudharyjatin115.pixelia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.chaudharyjatin115.pixelia.domain.model.MediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelectionMode && isSelected) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "thumbnail_selection_scale"
    )

    val animatedCornerRadius by animateFloatAsState(
        targetValue = if (isSelectionMode && isSelected) 20f else 12f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
        label = "thumbnail_corner_radius"
    )
    val dynamicShape = RoundedCornerShape(animatedCornerRadius.dp)

    val selectionBorder = if (isSelectionMode && isSelected) {
        BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
    } else null

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .aspectRatio(1f)
            .clip(dynamicShape)
            .then(if (selectionBorder != null) Modifier.border(selectionBorder, dynamicShape) else Modifier)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics {
                this.role = Role.Button
                this.contentDescription = "${if (item.isVideo) "Video" else "Photo"}, ${item.name}${if (isSelected) ", selected" else ""}"
            }
    ) {
        val imageRequest = remember(item.uri, item.id) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .size(320, 320)
                .precision(coil3.size.Precision.INEXACT)
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .crossfade(150)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelectionMode && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            )
        }

        if (isSelectionMode) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .border(1.5.dp, Color.White, CircleShape)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color.White
                    )
                    if (item.formattedDuration.isNotBlank()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.formattedDuration,
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (item.isFavorite && !isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.50f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Favorited",
                    modifier = Modifier.size(13.dp),
                    tint = Color(0xFFFF4D4D)
                )
            }
        }
    }
}
