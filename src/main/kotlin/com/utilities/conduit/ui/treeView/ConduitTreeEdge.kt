package com.utilities.conduit.ui.treeView

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun ConduitTreeEdge(
    start: Offset,
    end: Offset,
    leadsToCursor: Boolean
) {
    Canvas(Modifier.fillMaxSize()) {
        val dy = end.y - start.y
        val controlOffset = -dy * 0.45f

        val control2 = Offset(
            end.x,
            end.y - controlOffset
        )

        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                start.x,
                start.y + controlOffset,
                control2.x,
                control2.y,
                end.x,
                end.y
            )
        }

        // Bézier edge
        drawPath(
            path = path,
            color = Color.DarkGray,
            style = Stroke(width = 1.5f),
            alpha = if (leadsToCursor) 1f else 0.25f
        )
    }
}
