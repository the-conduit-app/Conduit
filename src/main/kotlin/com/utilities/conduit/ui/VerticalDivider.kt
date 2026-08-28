package com.utilities.conduit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s64
import org.intellij.lang.annotations.JdkConstants
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
            .width(8.dp)
            .pointerInput(totalWidth) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    if (totalWidth > 0) {
                        val deltaFraction = dragAmount.x / totalWidth

                        onLeftFractionChange(
                            (currentLeftFraction + deltaFraction)
                                .coerceIn(0.20f, 0.50f)
                        )
                    }
                }
            },
            contentAlignment = Alignment.Center
        ) {
        // Divider line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .pointerHoverIcon(
                    PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
                )
                .width(1.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.plasma_s64),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.75f
            )
        }

        // Grab handle
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )
        )
    }
}
