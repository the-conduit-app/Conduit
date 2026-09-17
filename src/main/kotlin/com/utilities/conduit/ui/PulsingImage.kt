package com.utilities.conduit.ui

import androidx.compose.animation.core.*
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
import conduit.generated.resources.Res
import conduit.generated.resources.cycle_branches
import org.jetbrains.compose.resources.painterResource

// Currently only used by ChatView to display an icon next to branching
// nodes.
@Composable
fun PulsingImage(
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
        painter = painterResource(Res.drawable.cycle_branches),
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
