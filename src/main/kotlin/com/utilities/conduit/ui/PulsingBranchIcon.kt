package com.utilities.conduit.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import org.jetbrains.compose.resources.painterResource
import conduit.generated.resources.Res
import conduit.generated.resources.cycleBranches

@Composable
fun PulsingBranchIcon(
    modifier: Modifier = Modifier,
    pulseDuration: Int = 1000,
    onClick: (() -> Unit)? = null
) {
    val transition = rememberInfiniteTransition()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.5f,
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

    val direction by animateFloatAsState(
        targetValue = if (isHovered) 1f else -1f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10*pulseDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    Image(
        painter = painterResource(Res.drawable.cycleBranches),
        contentDescription = "Branches",
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = opacity
                rotationZ = rotation * direction
            }
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null
            ) {
                onClick?.invoke()
            }
    )
}
