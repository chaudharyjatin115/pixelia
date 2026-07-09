package com.chaudharyjatin115.pixelia.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

object FrostedGlassDefaults {
    val BlurRadius: Dp = 32.dp
    const val NoiseFactor: Float = 0.03f

    @Composable
    fun style(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val tintColor = if (isDark) {
            Color(0x30121218)
        } else {
            Color(0x50FFFFFF)
        }

        return HazeStyle(
            backgroundColor = MaterialTheme.colorScheme.background,
            blurRadius = BlurRadius,
            noiseFactor = NoiseFactor,
            tints = listOf(HazeTint(tintColor))
        )
    }

    @Composable
    fun containerBackground(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Color(0x20121216)
        } else {
            Color(0x25FFFFFF)
        }
    }

    @Composable
    fun border(): BorderStroke {
        val isDark = isSystemInDarkTheme()
        val borderColor = if (isDark) {
            Color.White.copy(alpha = 0.22f)
        } else {
            Color.White.copy(alpha = 0.65f)
        }
        return BorderStroke(1.dp, borderColor)
    }

    @Composable
    fun unselectedContentColor(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Color.White.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    /**
     * Dedicated frosted glass style for the fullscreen photo viewer.
     * Always provides a dark translucent frosted glass pill so photos underneath blur dynamically,
     * while white icons and text maintain 100% contrast in both Light and Dark modes.
     */
    @Composable
    fun photoViewerStyle(): HazeStyle {
        return HazeStyle(
            backgroundColor = Color.Black,
            blurRadius = 32.dp,
            noiseFactor = 0.03f,
            tints = listOf(HazeTint(Color(0x35000000)))
        )
    }

    fun photoViewerBackground(): Color = Color(0x35121214)

    fun photoViewerBorder(): BorderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
}
