package com.utilities.conduit.ui.splashScreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.utilities.conduit.ui.Sounds
import conduit.generated.resources.Res
import conduit.generated.resources.enter_conduit
import conduit.generated.resources.open_conduit
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds


// Essentially mostly duplicated code from PulsingImage.kt (TODO ripe for a refactor)

// Shows the entry spinner portal while the conduit is initializing.
@Composable
fun SplashSpinner(
    modifier: Modifier = Modifier,
    isReady: Boolean,
    pulseDuration: Int = 20_000,
    isShaVerified: Boolean = false,
    onReady: (() -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition()
    val interactionSource = remember { MutableInteractionSource() }
    var fadingOut by remember { mutableStateOf(false) }
    val liquidState = rememberLiquidState()

    // When sha check passes on the system expert, this transitions from 0 to 1
    val shaCheckAlpha by animateFloatAsState(
        targetValue = if (isShaVerified) 1f else 0f,
        animationSpec = tween(400),
        label = "verifiedAlpha"
    )

    LaunchedEffect(Unit) {
        Sounds.Space.play()
    }
    LaunchedEffect(isReady) { // Sound to play when ready
        if (isReady) Sounds.Ready.play()
    }
    LaunchedEffect(fadingOut) {
        if (fadingOut) {
            Sounds.EnterChime.play()
            Sounds.Space.fadeOut()
            delay(2000.milliseconds)
            onReady?.invoke()
        }
    }

    val scale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val opacity by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Smoothly changes rotational direction when Conduit becomes ready.
    val direction by animateFloatAsState(
        targetValue = if (isReady) 1f else -1f,
        animationSpec = tween(
            durationMillis = 2000,
            easing = FastOutSlowInEasing
        )
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10 * pulseDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadingOut) 0f else 1f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseInOut
        ),
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (!isShaVerified || shaCheckAlpha < 1f) {
            Image(
                painter = painterResource(Res.drawable.open_conduit),
                contentDescription = "Open/Enter Conduit",
                modifier = Modifier
                    .liquefiable(liquidState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = opacity * fadeAlpha * (1f - shaCheckAlpha)
                        rotationZ = rotation * direction
                    }
            )
        }

        if (isShaVerified || shaCheckAlpha > 0f) {
            Image(
                painter = painterResource(Res.drawable.enter_conduit),
                contentDescription = "Enter Conduit",
                modifier = Modifier
                    .liquefiable(liquidState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = opacity * fadeAlpha * shaCheckAlpha
                        rotationZ = rotation * direction
                    }
            )
        }

        Box(
            modifier = Modifier
                .size(512.dp, 512.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !fadingOut
                ) {
                    if (isReady) {
                        fadingOut = true
                    } else {
                        Sounds.Knock.play()
                    }
                }
                .liquid(liquidState) {
                    frost = 0.dp
                    shape = RectangleShape
                    refraction = .25f
                    curve = 1f
                    edge = 0f
                    tint = Color.Transparent
                    saturation = 1f
                    dispersion = 0f
                    contrast = 1f
                }
        )
    }
}
