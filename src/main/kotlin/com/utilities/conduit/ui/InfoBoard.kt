package com.utilities.conduit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import org.jetbrains.compose.resources.painterResource
import io.github.fletchmckee.liquid.liquid
import kotlinx.serialization.json.JsonNull.content

@Composable
fun InfoBoard(
    modifier: Modifier = Modifier,
    outerShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    innerShape: RoundedCornerShape = RoundedCornerShape(12.dp),
    innerPadding: Dp = 6.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(outerShape)
            .border(
                1.dp,
                Color.Red.copy(alpha = 0.5f),
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

        // Inner reading surface
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .clip(innerShape)
                .border(
                    1.dp,
                    Color.Cyan.copy(alpha = 0.8f),
                    innerShape
                )
        ) {
            // Content area
            Box(
                modifier = Modifier
                    .liquid(LocalLiquidState.current) {
                        frost = 0.dp // 10
                        shape = outerShape
                        refraction = .75f // 0.18f
                        curve = .5f // 0.22f
                        edge = 0f // 0.30f
                        tint = Color.White.copy(alpha = 0.3f)
                    }
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
