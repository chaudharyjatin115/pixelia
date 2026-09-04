package com.chaudharyjatin115.pixelia.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.chaudharyjatin115.pixelia.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ExifMetadata(
    val cameraModel: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val software: String? = null
) {
    val hasCameraDetails: Boolean
        get() = !cameraModel.isNullOrBlank() || !aperture.isNullOrBlank() ||
                !shutterSpeed.isNullOrBlank() || !iso.isNullOrBlank() || !focalLength.isNullOrBlank()

    val hasLocation: Boolean
        get() = latitude != null && longitude != null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaInfoSheet(
    item: MediaItem,
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var exifData by remember(item.uri) { mutableStateOf<ExifMetadata?>(null) }

    LaunchedEffect(item.uri) {
        if (!item.isVideo) {
            exifData = extractExifMetadata(context, item.uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(if (item.dateTaken > 0) item.dateTaken else item.dateModified))

            InfoRow(
                icon = Icons.Rounded.Description,
                title = item.name,
                subtitle = item.mimeType
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            InfoRow(
                icon = Icons.Rounded.CalendarToday,
                title = formattedDate,
                subtitle = "Date taken"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            val mediaIcon = if (item.isVideo) Icons.Rounded.Videocam else Icons.Rounded.Image
            val resolutionStr = if (item.resolutionText.isNotBlank()) {
                val mp = if (item.width > 0 && item.height > 0) {
                    val megaPixels = (item.width.toLong() * item.height) / 1_000_000.0
                    String.format(Locale.getDefault(), " (%.1f MP)", megaPixels)
                } else ""
                "${item.resolutionText}$mp • ${item.formattedSize}"
            } else {
                item.formattedSize
            }

            InfoRow(
                icon = mediaIcon,
                title = resolutionStr,
                subtitle = if (item.isVideo && item.formattedDuration.isNotBlank()) {
                    "Duration: ${item.formattedDuration}"
                } else {
                    "Dimensions and file size"
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            InfoRow(
                icon = Icons.Rounded.Folder,
                title = item.bucketName.ifBlank { "Internal Storage" },
                subtitle = "Album location"
            )

            // EXIF Camera Information
            exifData?.takeIf { it.hasCameraDetails }?.let { exif ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = exif.cameraModel ?: "Camera Info",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        exif.software?.let { sw ->
                            Text(
                                text = "Software: $sw",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp)
                ) {
                    exif.aperture?.let { ExifChip(label = it) }
                    exif.shutterSpeed?.let { ExifChip(label = it) }
                    exif.iso?.let { ExifChip(label = it) }
                    exif.focalLength?.let { ExifChip(label = it) }
                    exif.flash?.let { ExifChip(label = it) }
                }
            }

            // Location / GPS Information
            exifData?.takeIf { it.hasLocation }?.let { exif ->
                val lat = exif.latitude!!
                val lng = exif.longitude!!

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatGpsCoordinates(lat, lng),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        exif.altitude?.let { alt ->
                            Text(
                                text = "Altitude: ${alt.roundToInt()}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            text = "GPS Location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = { openLocationInMaps(context, lat, lng) },
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open in Maps",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Maps")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExifChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun extractExifMetadata(context: Context, uri: Uri): ExifMetadata = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)

            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
            val cameraModel = when {
                !model.isNullOrBlank() && !make.isNullOrBlank() -> {
                    if (model.startsWith(make, ignoreCase = true)) model else "$make $model"
                }
                !model.isNullOrBlank() -> model
                !make.isNullOrBlank() -> make
                else -> null
            }

            val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
            val aperture = if (fNumber > 0) "f/${String.format(Locale.US, "%.1f", fNumber)}" else null

            val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            val shutterSpeed = if (exposureTime > 0) {
                if (exposureTime < 1.0) {
                    val denominator = (1.0 / exposureTime).roundToInt()
                    "1/${denominator}s"
                } else {
                    "${String.format(Locale.US, "%.1f", exposureTime)}s"
                }
            } else null

            val isoVal = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            val iso = if (!isoVal.isNullOrBlank()) "ISO $isoVal" else null

            val focalLengthVal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            val focalLength = if (focalLengthVal > 0) "${String.format(Locale.US, "%.1f", focalLengthVal)}mm" else null

            val flashVal = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
            val flash = if (flashVal != -1) {
                if ((flashVal and 1) != 0) "Flash fired" else "No flash"
            } else null

            val latLong = exif.latLong
            val lat = latLong?.getOrNull(0)
            val lng = latLong?.getOrNull(1)

            val alt = exif.getAttributeDouble(ExifInterface.TAG_GPS_ALTITUDE, 0.0).let { if (it != 0.0) it else null }
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim()?.takeIf { it.isNotBlank() }

            ExifMetadata(
                cameraModel = cameraModel,
                aperture = aperture,
                shutterSpeed = shutterSpeed,
                iso = iso,
                focalLength = focalLength,
                flash = flash,
                latitude = lat,
                longitude = lng,
                altitude = alt,
                software = software
            )
        } ?: ExifMetadata()
    } catch (e: java.io.IOException) {
        ExifMetadata()
    } catch (e: SecurityException) {
        ExifMetadata()
    } catch (e: IllegalArgumentException) {
        ExifMetadata()
    }
}

private fun formatGpsCoordinates(lat: Double, lng: Double): String {
    val latDirection = if (lat >= 0) "N" else "S"
    val lngDirection = if (lng >= 0) "E" else "W"
    return String.format(Locale.US, "%.4f° %s, %.4f° %s", Math.abs(lat), latDirection, Math.abs(lng), lngDirection)
}

private fun openLocationInMaps(context: Context, lat: Double, lng: Double) {
    try {
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, geoUri)
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        // Safe fallback when no map or geo intent handler app is installed on the device
    } catch (e: SecurityException) {
        // Safe fallback when activity launch is restricted by device policy
    }
}
