package org.example.glassEffects

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun GlassHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var capturedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var hostCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    CapturedContent(
        modifier = modifier,
        onPositioned = { hostCoordinates = it },
        onCaptured = { capturedImage = it }
    ) {
        CompositionLocalProvider(
            LocalGlassBackdrop provides
                    capturedImage?.let { image ->
                        hostCoordinates?.let { coordinates ->
                            GlassBackdrop(image, coordinates)
                        }
                    }
        ) {
            content()
        }
    }
}

@Composable
private fun CapturedContent(
    modifier: Modifier = Modifier,
    onPositioned: (LayoutCoordinates) -> Unit,
    onCaptured: (ImageBitmap) -> Unit,
    content: @Composable () -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    var ready by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates)

                ready = coordinates.size.width > 0 &&
                        coordinates.size.height > 0
            }
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
    ) {
        content()
    }

    LaunchedEffect(ready) {
        if (ready) {
            withFrameNanos { }

            if (graphicsLayer.size.width > 0f &&
                graphicsLayer.size.height > 0f
            ) {
                onCaptured(graphicsLayer.toImageBitmap())
            }
        }
    }
}
