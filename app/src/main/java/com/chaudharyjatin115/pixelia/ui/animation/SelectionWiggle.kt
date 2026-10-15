package com.chaudharyjatin115.pixelia.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberSelectionWiggle(selected: Boolean, angle: Float): State<Float> {
    val rotation = remember { Animatable(0f) }
    var wasSelected by remember { mutableStateOf(selected) }

    LaunchedEffect(selected) {
        if (selected && !wasSelected) {
            rotation.animateTo(angle, tween(durationMillis = 80))
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.32f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else if (!selected) {
            rotation.snapTo(0f)
        }
        wasSelected = selected
    }

    return rotation.asState()
}
