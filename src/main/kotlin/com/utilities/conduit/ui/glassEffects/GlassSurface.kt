package org.example.glassEffects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.Data
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.jetbrains.skia.Shader

data class GlassBackdrop(
    val image: ImageBitmap,
    val coordinates: LayoutCoordinates
)
val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    blurRadius: Float = 12f,
    tintAlpha: Float = 0.08f,
    borderAlpha: Float = 0.18f,
    cornerRadius: Dp = 12.dp,
    lensStrength: Float = 0f,
    scale: Float = 1f,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val backdrop = LocalGlassBackdrop.current
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .clip(shape)
            .onGloballyPositioned { coordinates = it }
            .background(Color.White.copy(alpha = tintAlpha))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = shape
            )
    ) {
        backdrop?.image?.let { image ->

            coordinates?.let { surfaceCoordinates ->
                val hostCoordinates = backdrop.coordinates

                val topLeft = hostCoordinates.localPositionOf(
                    surfaceCoordinates,
                    Offset.Zero
                )

                val bounds = Rect(
                    left = topLeft.x,
                    top = topLeft.y,
                    right = topLeft.x + surfaceCoordinates.size.width,
                    bottom = topLeft.y + surfaceCoordinates.size.height
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            renderEffect = BlurEffect(
                                radiusX = blurRadius,
                                radiusY = blurRadius
                            )
                        }
                ) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint()

                        paint.shader = makeLensShader(
                            image = image,
                            width = size.width,
                            height = size.height,
                            sourceRect = bounds,
                            strength = lensStrength
                        )

                        canvas.drawRect(
                            Rect(0f, 0f, size.width, size.height),
                            paint
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.matchParentSize()
        ) {
            content()
        }
    }
}

private const val lensShaderSource = """
uniform shader image;
uniform float2 resolution;
uniform float4 sourceRect;
uniform float strength;

half4 main(float2 p) {
    float2 uv = p / resolution;
    float2 centered = uv - 0.5;

    float r = length(centered);
    float2 direction = r > 0.0 ? centered / r : float2(0.0);

    float warp = 1.0 + strength * r * r;

    float2 sampleUv = 0.5 + direction * r * warp;

    float2 samplePoint =
        sourceRect.xy + sampleUv * sourceRect.zw;

    return image.eval(samplePoint);
}
"""

private fun makeLensEffect(): RuntimeEffect =
    RuntimeEffect.makeForShader(lensShaderSource)

private fun makeLensShader(
    image: ImageBitmap,
    width: Float,
    height: Float,
    sourceRect: Rect,
    strength: Float
): Shader {
    val imageShader = image.asSkiaBitmap().makeShader()

    val uniforms = ByteBuffer
        .allocate(28)
        .order(ByteOrder.nativeOrder())
        .apply {
            putFloat(width)
            putFloat(height)

            putFloat(sourceRect.left)
            putFloat(sourceRect.top)
            putFloat(sourceRect.width)
            putFloat(sourceRect.height)

            putFloat(strength)
        }
        .array()

    val data = Data.makeFromBytes(uniforms)

    return makeLensEffect().makeShader(
        data,
        arrayOf(imageShader),
        null
    )
}
