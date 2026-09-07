package com.utilities.conduit.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull.content
import kotlin.math.roundToInt

// This is used for showing messages on hover over Tree nodes in TreeView (nowhere else currently)

val LocalMessagePanelController = staticCompositionLocalOf<MessagePanelController> {
    error("MessagePanel not provided")
}
class MessagePanelController(private val scope: CoroutineScope) {
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
fun MessagePanel(
    panel: MessagePanelController,
    fontSize: TextUnit = 14.sp,
    maxLines: Int = Int.MAX_VALUE
) {
    if (panel.opacity <= 0f) return

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    panel.position.x.roundToInt(),
                    panel.position.y.roundToInt()
                )
            }
            .alpha(panel.opacity)
    ) {
        var hasOverflow by remember(panel.text) {
            mutableStateOf(false)
        }

        val panelShape = RoundedCornerShape(12.dp)
        InfoBoard(
            modifier = Modifier.widthIn(max = 400.dp),
            outerShape = panelShape,
            innerShape = panelShape,
            innerPadding = 10.dp,
            contentPadding = 10.dp,
            content = {
                Column {
                    if (panel.title.isNotEmpty()) {
                        Text(
                            text = panel.title,
                            fontSize = 10.sp,
                            color = Color.Gray
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
        )
    }
}
