package com.utilities.conduit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s64
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import org.jetbrains.compose.resources.painterResource
import java.awt.Cursor

@Composable
fun VerticalDivider(
    totalWidth: Int,
    leftFraction: Float,
    onLeftFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLeftFraction by rememberUpdatedState(leftFraction)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(5.dp)
            .pointerInput(totalWidth) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    if (totalWidth > 0) {
                        val deltaFraction = dragAmount.x / totalWidth

                        onLeftFractionChange(
                            (currentLeftFraction + deltaFraction / 1f)
                                .coerceIn(0.20f, 0.50f)
                        )
                    }
                }
            }
            .pointerHoverIcon(
                PointerIcon(
                    Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // Divider line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.plasma_s64),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = .75f
            )
        }

        // Grab handle
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF007C91).copy(alpha = 0.45f)
                ),
            contentAlignment = Alignment.Center
        ) {}
    }
}
