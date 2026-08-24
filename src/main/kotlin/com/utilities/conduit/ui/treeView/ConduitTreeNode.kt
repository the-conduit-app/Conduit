package com.utilities.conduit.ui.treeView

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.dk.kuiver.model.KuiverNode
import conduit.generated.resources.Res
import conduit.generated.resources.treeViewNode
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.State
import com.utilities.conduit.ui.treeView.NodeAnimations.nodeBlinking
import com.utilities.conduit.ui.treeView.NodeAnimations.nodePulsing
import com.utilities.conduit.ui.treeView.NodeAnimations.nodeRotation

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConduitTreeNode(
    kuiverNode: KuiverNode,
    isActive: Boolean,
    isCursor: Boolean = false,
    onHoverChanged: (Boolean, Offset) -> Unit = { _, _ -> },
    onClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")


    Box(
        modifier = Modifier
            .size(24.dp)
            .onGloballyPositioned {
                coordinates = it
            }
            .onPointerEvent(PointerEventType.Enter) { event ->
                isHovered = true
                coordinates?.let {
                    val localPosition = event.changes.first().position
                    val rootPosition = it.localToRoot(localPosition)
                    onHoverChanged(true, rootPosition)
                }
            }
            .onPointerEvent(PointerEventType.Exit) { event ->
                isHovered = false
                onHoverChanged(false, Offset.Zero)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = if (isHovered) nodePulsing else 1f
                    scaleY = if (isHovered) nodePulsing else 1f
                    alpha = if (isCursor) nodeBlinking else 1f
                    rotationZ = if (isActive) nodeRotation else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.treeViewNode),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.24f))
            )

//            Text(
//                text = node.id,
//                style = TextStyle(fontSize = 8.sp)
//            )
        }
    }
}

private object NodeAnimations {

    val nodeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                easing = LinearEasing
            )
        ),
        label = "nodeRotation"
    )

    val nodeBlinking by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val nodePulsing by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulse"
    )

    @Composable
    fun rotation(transition: InfiniteTransition): State<Float> =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                )
            ),
            label = "nodeRotation"
        )

    @Composable
    fun blink(transition: InfiniteTransition): State<Float> =
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nodeBlink"
        )

    @Composable
    fun generatingPulse(transition: InfiniteTransition): State<Float> =
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nodeGeneratingPulse"
        )
}
