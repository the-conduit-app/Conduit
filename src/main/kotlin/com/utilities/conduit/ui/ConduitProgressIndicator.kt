package com.utilities.conduit.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun ConduitProgressIndicator(
    images: List<Painter>,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()
    require(images.size >= 2)

    var n by remember { mutableIntStateOf(0) }
    var revealFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(images) {
        while (true) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 4000,
                    easing = EaseInOut
                )
            ) { value, _ ->
                revealFraction = value
            }

            n = (n + 1) % images.size
        }
    }

    val image1 = images[n]
    val image2 = images[(n + 1) % images.size]

    Box(modifier
        .clip(CircleShape)
    ) {
        Image(
            painter = image1,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = image2,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val radius = size.minDimension / 2f * revealFraction

                    val path = Path().apply {
                        addOval(
                            Rect(
                                left = size.width / 2f - radius,
                                top = size.height / 2f - radius,
                                right = size.width / 2f + radius,
                                bottom = size.height / 2f + radius
                            )
                        )
                    }

                    onDrawWithContent {
                        clipPath(path) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                }
        )
    }
}
