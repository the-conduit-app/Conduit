package org.example.glassEffects

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.utilities.conduit.debug.Trace
import jdk.internal.org.commonmark.internal.Bracket.image
/*
@Composable
fun GlassHost(
    modifier: Modifier = Modifier,
    controller: GlassHostController,
    content: @Composable () -> Unit
) {
    var capturedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var hostCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Trace.log("GlassSurface RECEIVED controller=${controller.hashCode()}")

    CaptureBackground(
        modifier = modifier,
        captureRequestVersion = controller.captureRequestVersion,
        onPositioned = { hostCoordinates = it },
        onCaptureComplete = { image ->
            capturedImage = image
            controller.captureCompleted()
        }
    ) {
        CompositionLocalProvider(
            LocalGlassBackdrop provides
                    capturedImage?.let { image ->
                        hostCoordinates?.let { coordinates ->
                            GlassBackdrop(image, coordinates)
                        }
                    },
            LocalGlassHostController provides controller
        ) {
            Trace.log("GlassHost PROVIDING controller=${controller.hashCode()}")
            content()
        }
    }
}

class GlassHostController {
    internal var captureRequestVersion by mutableIntStateOf(0)
        private set

    private var onCaptureComplete: (() -> Unit)? = null

    fun captureBackground(onCaptureComplete: () -> Unit) {
        this.onCaptureComplete = onCaptureComplete
        captureRequestVersion++
    }

    internal fun captureCompleted() {
        onCaptureComplete?.invoke()
        onCaptureComplete = null
    }
}

@Composable
private fun CaptureBackground(
    modifier: Modifier = Modifier,
    captureRequestVersion: Int,
    onPositioned: (LayoutCoordinates) -> Unit,
    onCaptureComplete: (ImageBitmap) -> Unit,
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

    LaunchedEffect(ready, captureRequestVersion) {
        if (!ready) return@LaunchedEffect

        withFrameNanos { }

        if (graphicsLayer.size.width > 0f && graphicsLayer.size.height > 0f) {
            onCaptureComplete(graphicsLayer.toImageBitmap())
        }
    }
}
*/
