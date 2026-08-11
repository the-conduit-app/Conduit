package com.utilities.conduit

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer

enum class ArrowDirection {
    Down, Up, Left, Right
}

@Composable
fun PulsingArrow(modifier: Modifier = Modifier, pulseDuration: Int = 300,
                 direction: ArrowDirection = ArrowDirection.Right,
                 onClick: (() -> Unit)? = null) {
    val transition = rememberInfiniteTransition()
    val rotationAngle = when (direction) {
        ArrowDirection.Right -> 0f
        ArrowDirection.Down -> 90f
        ArrowDirection.Left -> 180f
        ArrowDirection.Up -> 270f
    }

    val opacity by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = "Show branch",
        modifier = modifier
            .rotate(rotationAngle)
            .graphicsLayer { alpha = opacity }
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
    )
}
