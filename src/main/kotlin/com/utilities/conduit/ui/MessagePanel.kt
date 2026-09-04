package com.utilities.conduit.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

// This is used for showing messages on hover over Tree nodes in TreeView (nowhere else currently)

val LocalMessagePanel = staticCompositionLocalOf<MessagePanel> {
    error("MessagePanel not provided")
}

class MessagePanel(private val scope: CoroutineScope) {

    var text by mutableStateOf("")
        private set

    var title by mutableStateOf("")
        private set

    var position by mutableStateOf(Offset.Zero)
        private set

    var opacity by mutableFloatStateOf(0f)
        private set

    private var job: Job? = null

    fun show(
        title: String,
        text: String,
        position: Offset
    ) {
        job?.cancel()

        this.text = text
        this.title = title
        this.position = position

        job = scope.launch {
            animate(
                initialValue = opacity,
                targetValue = 1f,
                animationSpec = tween(180)
            ) { value, _ ->
                opacity = value
            }
        }
    }

    fun hide() {
        job?.cancel()

        job = scope.launch {
            animate(
                initialValue = opacity,
                targetValue = 0f,
                animationSpec = tween(120)
            ) { value, _ ->
                opacity = value
            }
        }
    }
}

@Composable
fun MessagePanelView(
    panel: MessagePanel,
    fontSize: TextUnit = 14.sp,
    maxLines: Int = Int.MAX_VALUE
) {
    if (panel.opacity <= 0f) return

    val outerShape = RoundedCornerShape(20.dp)
    val innerShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    panel.position.x.roundToInt(),
                    panel.position.y.roundToInt()
                )
            }
            .alpha(panel.opacity)
            .clip(outerShape)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.45f),
                outerShape
            )
    ) {
        Image(
            painter = painterResource(Res.drawable.plasma_s1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.75f
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.08f))
        )

        Box(
            modifier = Modifier
                .padding(6.dp)
                .clip(innerShape)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.5f),
                    innerShape
                )
                .background(Color.White.copy(alpha = 0.50f))
                .widthIn(max = 400.dp)
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp)
        ) {
            var hasOverflow by remember(panel.text) { mutableStateOf(false) }
            Column {
                if (panel.title.isNotEmpty()) {
                    Text(
                        text = panel.title,
                        fontSize = 10.sp,
                        color = Color.Gray //// .copy(alpha = .65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = panel.text.trim(),
                    softWrap = true,
                    fontSize = fontSize,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        hasOverflow = result.hasVisualOverflow
                    }
                )

                if (hasOverflow) {
                    Text(
                        text = "…",
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }
        }
    }
}
