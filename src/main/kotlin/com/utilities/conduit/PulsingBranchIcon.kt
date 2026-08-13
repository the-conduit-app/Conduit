package com.utilities.conduit

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun PulsingBranchIcon(modifier: Modifier = Modifier, pulseDuration: Int = 1000, onClick: (() -> Unit)? = null) {
    val transition = rememberInfiniteTransition()

    val opacity by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        imageVector = Icons.AutoMirrored.Filled.CallSplit,
        contentDescription = "Branches",
        modifier = modifier
            .graphicsLayer { alpha = opacity }
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
    )
}
