package org.example.glassEffects

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassButton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val animatedBlur by animateFloatAsState(
        targetValue = if (pressed) 25f else 50f,
        animationSpec = tween(120),
        label = "glassButtonBlur"
    )

    val animatedLens by animateFloatAsState(
        targetValue = if (pressed) 0.20f else 0.15f,
        animationSpec = tween(120),
        label = "glassButtonLens"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "glassButtonScale"
    )

    val animatedShadow by animateDpAsState(
        targetValue = if (pressed) 2.dp else 12.dp,
        animationSpec = tween(120),
        label = "glassButtonShadow"
    )

    val animatedOffset by animateDpAsState(
        targetValue = if (pressed) 5.dp else 0.dp,
        animationSpec = tween(120),
        label = "glassButtonOffset"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                            onClick()
                        } finally {
                            pressed = false
                        }
                    }
                )
            }
    ) {
        GlassSurface(
            blurRadius = animatedBlur,
            lensStrength = animatedLens,
            scale = animatedScale,
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = animatedShadow,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .offset(y = animatedOffset)
        ) {
            content()
        }
    }
}
