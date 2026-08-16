package com.utilities.conduit

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.material.icons.filled.Sync

@Composable
fun PulsingBranchIcon(
    modifier: Modifier = Modifier,
    pulseDuration: Int = 1000,
    onClick: (() -> Unit)? = null
) {
    val transition = rememberInfiniteTransition()

    val scale by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val opacity by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2*pulseDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    Icon(
        imageVector = Icons.Filled.Sync,
        contentDescription = "Branches",
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = opacity
                rotationZ = rotation
            }
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
    )
}
