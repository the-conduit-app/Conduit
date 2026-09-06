package com.utilities.conduit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import org.jetbrains.compose.resources.painterResource
import io.github.fletchmckee.liquid.liquid

@Composable
fun InfoBoard(
    modifier: Modifier = Modifier,
    outerShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    innerShape: RoundedCornerShape = RoundedCornerShape(14.dp),
    innerPadding: Dp = 6.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(outerShape)
            .liquid(LocalLiquidState.current) {
                frost = 10.dp
                shape = outerShape
                refraction = 0.18f
                curve = 0.22f
                edge = 0.30f
                tint = Color.White.copy(alpha = 0.16f)
            }
            .border(
                1.dp,
                Color.White.copy(alpha = 0.45f),
                outerShape
            )
    ) {
        // Plasma background
        Image(
            painter = painterResource(Res.drawable.plasma_s1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.75f
        )

        // Subtle wash
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.08f))
        )

        // Inner reading surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clip(innerShape)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.5f),
                    innerShape
                )
        ) {
            // Content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                content()
            }

            // Overlay area: positioned relative to the entire
            // inner reading surface, outside content padding.
            overlay()
        }
    }
}
