package com.utilities.conduit.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SubtleScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    if (scrollState.maxValue <= 0) return

    val viewportHeight = scrollState.viewportSize.toFloat()
    val contentHeight = viewportHeight + scrollState.maxValue

    val thumbFraction =
        (viewportHeight / contentHeight)
            .coerceIn(0.05f, 1f)

    val scrollFraction =
        scrollState.value.toFloat() / scrollState.maxValue

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(
                top = 8.dp,
                bottom = 8.dp,
                end = 5.dp
            )
    ) {
        val thumbHeight = maxHeight * thumbFraction

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = (maxHeight - thumbHeight) * scrollFraction
                )
                .width(3.dp)
                .height(thumbHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Color.Black.copy(alpha = 0.20f)
                )
        )
    }
}
