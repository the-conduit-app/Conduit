package com.utilities.conduit.ui.chatView

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.Node
import com.utilities.conduit.ui.ConduitProgressIndicator
import com.utilities.conduit.ui.LocalActions
import com.utilities.conduit.ui.LocalLiquidState
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import conduit.generated.resources.plasma_s64
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import org.jetbrains.compose.resources.painterResource
import java.awt.SystemColor.text

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(
    node: Node,
    textInProgress: String?,
    isCursor: Boolean,
    contextMenuItems: (() -> List<ContextMenuItem>)?
) {
    when (node.message?.authorType ?: AuthorType.SYSTEM) {
        AuthorType.USER -> UserMessageBubble(node, textInProgress, isCursor, contextMenuItems)
        AuthorType.ASSISTANT -> ExpertMessageBubble(node, textInProgress, isCursor, contextMenuItems)
        AuthorType.SYSTEM -> ExpertMessageBubble(node, textInProgress, isCursor, contextMenuItems)
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun UserMessageBubble(node: Node, textInProgress: String?, isCursor: Boolean, contextMenuItems: (() -> List<ContextMenuItem>)?) {
    val text = textInProgress ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donowatt" // an unknown amount of power
    var hasMore by remember { mutableStateOf(false) }

    val bubble: @Composable () -> Unit = {
        val liquidState = LocalLiquidState.current

        MessageBubbleSurface(
            liquidState,
            color = Color.Green,
            cornerRadius = 12.dp
        ) {
            if (textInProgress != null && text.isEmpty()) {
                ConduitProgressIndicator(
                    images = listOf(
                        painterResource(Res.drawable.plasma_s64),
                        painterResource(Res.drawable.plasma_s1)
                    ),
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = text.trim(),
                        fontSize = 14.sp,
                        maxLines = if (textInProgress == null && !isCursor) 5 else Int.MAX_VALUE,
                        onTextLayout = { result ->
                            hasMore = (textInProgress == null && result.hasVisualOverflow)
                        }
                    )

                    if (hasMore && !isCursor) {
                        val appActions = LocalActions.current
                        Text(
                            text = "… More",
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                appActions.showFullMessage(text)
                            }
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 10.sp,
                color = Color.Gray
            )
        )

        if (contextMenuItems != null) {
            ContextMenuArea(items = contextMenuItems) { bubble() }
        } else {
            bubble()
        }
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun ExpertMessageBubble(node: Node, textInProgress: String?, isCursor: Boolean, contextMenuItems: (() -> List<ContextMenuItem>)?) {
    val text = textInProgress ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donovich"
    var hasMore by remember { mutableStateOf(false) }
    val bubbleColor = if (node.message?.authorType == AuthorType.SYSTEM) Color.White else Color.Magenta

    val bubble: @Composable () -> Unit = {
        val liquidState = LocalLiquidState.current

        MessageBubbleSurface(
            liquidState = liquidState,
            color = bubbleColor,
            cornerRadius = 12.dp
        ) {
            if (textInProgress != null && text.isEmpty()) {
                ConduitProgressIndicator(
                    images = listOf(
                        painterResource(Res.drawable.plasma_s64),
                        painterResource(Res.drawable.plasma_s1)
                    ),
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = text.trim(),
                        fontSize = 14.sp,
                        maxLines = if (textInProgress == null && !isCursor) 5 else Int.MAX_VALUE,
                        onTextLayout = { result ->
                            hasMore = textInProgress == null && result.hasVisualOverflow
                        }
                    )

                    if (hasMore && !isCursor) {
                        val appActions = LocalActions.current
                        Text(
                            text = "… More",
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                appActions.showFullMessage(text)
                            }
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 10.sp,
                color = Color.Gray
            )
        )

        if (contextMenuItems != null) {
            ContextMenuArea(items = contextMenuItems) { bubble() }
        } else {
            bubble()
        }

        node.message?.responseTime?.let { ts ->
            Text(
                text = "Response took ${"%.1f".format(ts / 1000.0)}s",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            )
        }
    }
}

@Composable
private fun MessageBubbleSurface(
    liquidState: LiquidState,
    color: Color,
    cornerRadius: Dp,
    content: @Composable () -> Unit
) {
    val bubbleShape = RoundedCornerShape(cornerRadius)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquid(liquidState) {
                frost = 2.dp
                shape = bubbleShape
                refraction = .5f
                curve = 1f
                edge = .5f
                tint = color.copy(alpha = 0.2f)
                saturation = 1f
                dispersion = .5f
                contrast = 1f
            }
            .clip(bubbleShape)
            .padding(8.dp)
    ) {
        content()
    }
}
