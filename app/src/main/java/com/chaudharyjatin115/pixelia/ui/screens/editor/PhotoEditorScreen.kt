package com.chaudharyjatin115.pixelia.ui.screens.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Filter
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class EditorTab(val label: String) {
    CROP("Crop"),
    ROTATE("Rotate"),
    FILTER("Filters")
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT_4_3("3:4", 3f / 4f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    PORTRAIT_16_9("9:16", 9f / 16f),
    LANDSCAPE_16_9("16:9", 16f / 9f)
}

enum class EditorFilter(val displayName: String) {
    ORIGINAL("Original"),
    MONO("B&W"),
    WARM("Warm"),
    COOL("Cool"),
    VINTAGE("Vintage"),
    VIVID("Vivid")
}

@Composable
fun PhotoEditorScreen(
    item: MediaItem,
    onClose: () -> Unit,
    onSavedCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var activeTab by remember { mutableStateOf(EditorTab.CROP) }
    var selectedCropRatio by remember { mutableStateOf(CropAspectRatio.FREE) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var isFlippedHorizontally by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(EditorFilter.ORIGINAL) }

    // Normalized crop rectangle [0f..1f]
    var cropLeft by remember { mutableFloatStateOf(0.05f) }
    var cropTop by remember { mutableFloatStateOf(0.05f) }
    var cropRight by remember { mutableFloatStateOf(0.95f) }
    var cropBottom by remember { mutableFloatStateOf(0.95f) }

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotatedFilteredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            originalBitmap?.recycle()
            originalBitmap = null
            rotatedFilteredBitmap?.recycle()
            rotatedFilteredBitmap = null
        }
    }

    // Load initial bitmap with safe downsampling to prevent heap exhaustion
    LaunchedEffect(item.uri) {
        withContext(Dispatchers.IO) {
            val old = originalBitmap
            originalBitmap = decodeSampledBitmap(context, item.uri, maxDimension = 1600)
            if (old != null && old != originalBitmap) {
                old.recycle()
            }
        }
    }

    // Update rotated + filtered base image whenever rotation or filter changes
    LaunchedEffect(originalBitmap, rotationDegrees, isFlippedHorizontally, selectedFilter) {
        val src = originalBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val nextBmp = renderRotatedAndFiltered(
                source = src,
                rotationDegrees = rotationDegrees,
                isFlipped = isFlippedHorizontally,
                filter = selectedFilter
            )
            val prev = rotatedFilteredBitmap
            rotatedFilteredBitmap = nextBmp
            if (prev != null && prev != src && prev != nextBmp) {
                prev.recycle()
            }
        }
    }

    // Snap crop rect when an aspect ratio preset is selected
    LaunchedEffect(selectedCropRatio) {
        val ratio = selectedCropRatio.ratio
        if (ratio != null) {
            val centerWidth = 0.85f
            val centerHeight = (centerWidth / ratio).coerceIn(0.2f, 0.90f)
            val finalWidth = (centerHeight * ratio).coerceIn(0.2f, 0.90f)
            cropLeft = (0.5f - finalWidth / 2f).coerceIn(0.02f, 0.45f)
            cropRight = (0.5f + finalWidth / 2f).coerceIn(0.55f, 0.98f)
            cropTop = (0.5f - centerHeight / 2f).coerceIn(0.02f, 0.45f)
            cropBottom = (0.5f + centerHeight / 2f).coerceIn(0.55f, 0.98f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Edit Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        coroutineScope.launch {
                            val success = saveEditedCopy(
                                context = context,
                                sourceUri = item.uri,
                                rotation = rotationDegrees,
                                isFlipped = isFlippedHorizontally,
                                cropLeft = cropLeft,
                                cropTop = cropTop,
                                cropRight = cropRight,
                                cropBottom = cropBottom,
                                filter = selectedFilter
                            )
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "Saved as copy to Pictures/Edited", Toast.LENGTH_SHORT).show()
                                onSavedCopy()
                                onClose()
                            } else {
                                Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Copy", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Interactive Preview and Crop Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = rotatedFilteredBitmap
                if (bmp != null) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val viewWidth = maxWidth
                        val viewHeight = maxHeight
                        val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
                        val containerRatio = viewWidth.value / viewHeight.value

                        val (contentWidthDp, contentHeightDp) = if (containerRatio > bmpRatio) {
                            Pair(viewHeight * bmpRatio, viewHeight)
                        } else {
                            Pair(viewWidth, viewWidth / bmpRatio)
                        }

                        Box(
                            modifier = Modifier
                                .size(contentWidthDp, contentHeightDp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Preview",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Interactive crop box with free-size drag handles when in CROP tab
                            if (activeTab == EditorTab.CROP) {
                                val boxLeft = contentWidthDp * cropLeft
                                val boxTop = contentHeightDp * cropTop
                                val boxWidth = (contentWidthDp * (cropRight - cropLeft)).coerceAtLeast(40.dp)
                                val boxHeight = (contentHeightDp * (cropBottom - cropTop)).coerceAtLeast(40.dp)

                                // Semitransparent darkened dim outside crop box
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.38f))
                                )

                                // Active Crop Window
                                Box(
                                    modifier = Modifier
                                        .offset(x = boxLeft, y = boxTop)
                                        .size(boxWidth, boxHeight)
                                        .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                                        .pointerInput(Unit) {
                                            // Panning inside the crop box
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val dx = dragAmount.x / size.width.toFloat()
                                                val dy = dragAmount.y / size.height.toFloat()
                                                val currentW = cropRight - cropLeft
                                                val currentH = cropBottom - cropTop

                                                var newL = (cropLeft + dx).coerceIn(0f, 1f - currentW)
                                                var newT = (cropTop + dy).coerceIn(0f, 1f - currentH)
                                                cropLeft = newL
                                                cropRight = newL + currentW
                                                cropTop = newT
                                                cropBottom = newT + currentH
                                            }
                                        }
                                ) {
                                    // Rule of thirds grid lines
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.fillMaxWidth().height(0.7.dp).background(Color.White.copy(alpha = 0.4f)))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.fillMaxWidth().height(0.7.dp).background(Color.White.copy(alpha = 0.4f)))
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.fillMaxHeight().width(0.7.dp).background(Color.White.copy(alpha = 0.4f)))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.fillMaxHeight().width(0.7.dp).background(Color.White.copy(alpha = 0.4f)))
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    // Top-Left Corner Handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset((-12).dp, (-12).dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val dx = dragAmount.x / (contentWidthDp.toPx())
                                                    val dy = dragAmount.y / (contentHeightDp.toPx())
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.15f)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.15f)
                                                }
                                            }
                                    )

                                    // Top-Right Corner Handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(12.dp, (-12).dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val dx = dragAmount.x / (contentWidthDp.toPx())
                                                    val dy = dragAmount.y / (contentHeightDp.toPx())
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + 0.15f, 1f)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.15f)
                                                }
                                            }
                                    )

                                    // Bottom-Left Corner Handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .offset((-12).dp, 12.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val dx = dragAmount.x / (contentWidthDp.toPx())
                                                    val dy = dragAmount.y / (contentHeightDp.toPx())
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.15f)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.15f, 1f)
                                                }
                                            }
                                    )

                                    // Bottom-Right Corner Handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(12.dp, 12.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val dx = dragAmount.x / (contentWidthDp.toPx())
                                                    val dy = dragAmount.y / (contentHeightDp.toPx())
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + 0.15f, 1f)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.15f, 1f)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181818))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 8.dp)
            ) {
                // Tab-specific controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (activeTab) {
                        EditorTab.CROP -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CropAspectRatio.entries.forEach { ratio ->
                                    val isSelected = ratio == selectedCropRatio
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCropRatio = ratio },
                                        label = { Text(ratio.label) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }

                        EditorTab.ROTATE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { rotationDegrees = (rotationDegrees + 270) % 360 },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                                        contentDescription = "Rotate left",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(32.dp))

                                IconButton(
                                    onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                                        contentDescription = "Rotate right",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(32.dp))

                                IconButton(
                                    onClick = { isFlippedHorizontally = !isFlippedHorizontally },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isFlippedHorizontally) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Flip,
                                        contentDescription = "Flip horizontally",
                                        tint = if (isFlippedHorizontally) MaterialTheme.colorScheme.onPrimary else Color.White
                                    )
                                }
                            }
                        }

                        EditorTab.FILTER -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorFilter.entries.forEach { filter ->
                                    val isSelected = filter == selectedFilter
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(filter.displayName) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Tab Navigation: [ Crop | Rotate | Filters ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorTab.entries.forEach { tab ->
                        val isSelected = tab == activeTab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.70f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Transforms bitmap according to rotation, flip, and color filter.
 */
private fun renderRotatedAndFiltered(
    source: Bitmap,
    rotationDegrees: Int,
    isFlipped: Boolean,
    filter: EditorFilter
): Bitmap {
    val matrix = Matrix()
    if (isFlipped) {
        matrix.postScale(-1f, 1f)
    }
    if (rotationDegrees != 0) {
        matrix.postRotate(rotationDegrees.toFloat())
    }

    val rotated = Bitmap.createBitmap(
        source,
        0,
        0,
        source.width,
        source.height,
        matrix,
        true
    )

    val out = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cm = ColorMatrix()
    when (filter) {
        EditorFilter.ORIGINAL -> { /* Identity */ }
        EditorFilter.MONO -> {
            cm.setSaturation(0f)
        }
        EditorFilter.WARM -> {
            cm.set(
                floatArrayOf(
                    1.12f, 0f, 0f, 0f, 10f,
                    0f, 1.04f, 0f, 0f, 5f,
                    0f, 0f, 0.90f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        EditorFilter.COOL -> {
            cm.set(
                floatArrayOf(
                    0.92f, 0f, 0f, 0f, -8f,
                    0f, 1.02f, 0f, 0f, 0f,
                    0f, 0f, 1.15f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        EditorFilter.VINTAGE -> {
            cm.set(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0.90f, 0f, 0f
                )
            )
        }
        EditorFilter.VIVID -> {
            cm.setSaturation(1.4f)
        }
    }

    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(rotated, 0f, 0f, paint)

    return out
}

private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val w = boundsOptions.outWidth
        val h = boundsOptions.outHeight
        if (w <= 0 || h <= 0) return null

        var sampleSize = 1
        var longest = maxOf(w, h)
        while (longest > maxDimension * 1.2f) {
            sampleSize *= 2
            longest /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (e: java.io.IOException) {
        null
    } catch (e: OutOfMemoryError) {
        null
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

private suspend fun saveEditedCopy(
    context: Context,
    sourceUri: Uri,
    rotation: Int,
    isFlipped: Boolean,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    filter: EditorFilter
): Boolean = withContext(Dispatchers.IO) {
    try {
        val fullBmp = decodeSampledBitmap(context, sourceUri, maxDimension = 3840)
            ?: return@withContext false

        // 1. Rotate & Filter
        val baseTransformed = renderRotatedAndFiltered(
            source = fullBmp,
            rotationDegrees = rotation,
            isFlipped = isFlipped,
            filter = filter
        )
        if (baseTransformed != fullBmp) {
            fullBmp.recycle()
        }

        // 2. Crop to normalized bounds
        val startX = (cropLeft * baseTransformed.width).toInt().coerceIn(0, baseTransformed.width - 1)
        val startY = (cropTop * baseTransformed.height).toInt().coerceIn(0, baseTransformed.height - 1)
        val targetWidth = ((cropRight - cropLeft) * baseTransformed.width).toInt().coerceIn(1, baseTransformed.width - startX)
        val targetHeight = ((cropBottom - cropTop) * baseTransformed.height).toInt().coerceIn(1, baseTransformed.height - startY)

        val finalCropped = Bitmap.createBitmap(
            baseTransformed,
            startX,
            startY,
            targetWidth,
            targetHeight
        )
        if (finalCropped != baseTransformed) {
            baseTransformed.recycle()
        }

        val fileName = "edited_copy_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Edited")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val outUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false

        context.contentResolver.openOutputStream(outUri)?.use { output ->
            finalCropped.compress(Bitmap.CompressFormat.JPEG, 94, output)
        }
        finalCropped.recycle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(outUri, values, null, null)
        }

        true
    } catch (e: java.io.IOException) {
        false
    } catch (e: OutOfMemoryError) {
        false
    } catch (e: SecurityException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }
}
