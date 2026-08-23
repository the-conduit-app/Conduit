package com.utilities.conduit.ui.treeView

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.kuiver.model.KuiverNode
import conduit.generated.resources.Res
import conduit.generated.resources.treeViewNode
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConduitTreeNode(
    node: KuiverNode,
    isCursor: Boolean = false,
    onHoverChanged: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")

    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 1.25f else 1f,
        animationSpec = tween(500),
        label = "hoverScale"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .onPointerEvent(PointerEventType.Enter) {
                isHovered = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                isHovered = false
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
                    scaleX = hoverScale
                    scaleY = hoverScale
                    alpha = if (isCursor) cursorAlpha else 1f
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

@Composable
fun TidyTreeCursorNode() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")

    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = cursorAlpha),
                shape = CircleShape
            )
    )
}
