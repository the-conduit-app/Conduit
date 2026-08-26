package com.utilities.conduit.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.Node

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(
    node: Node,
    isCursor: Boolean,
    contextMenuItems: (() -> List<ContextMenuItem>)?
) {
    when (node.message?.author?.type ?: AuthorType.SYSTEM) {
        AuthorType.USER -> UserMessageBubble(node, isCursor, contextMenuItems)
        AuthorType.ASSISTANT -> ExpertMessageBubble(node, isCursor, contextMenuItems)
        AuthorType.SYSTEM -> SystemMessageBubble(node.message?.text ?: "Hmm... Wonder where this came from!")
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun UserMessageBubble(node: Node, isCursor: Boolean, contextMenuItems: (() -> List<ContextMenuItem>)?) {
    val textInProgress = node.message?.textInProgress?.value
    val text = textInProgress ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donowatt" // an unknown amount of power
    var hasMore by remember { mutableStateOf(false) }

    val bubble: @Composable () -> Unit = {
        MessageBubbleSurface(
            color = Color.Green,
            cornerRadius = 12.dp
        ) {
            if (textInProgress != null && text.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
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
private fun ExpertMessageBubble(node: Node, isCursor: Boolean, contextMenuItems: (() -> List<ContextMenuItem>)?) {
    val textInProgress = node.message?.textInProgress?.value
    val text = textInProgress ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donovich"
    var hasMore by remember { mutableStateOf(false) }

    val bubble: @Composable () -> Unit = {
        MessageBubbleSurface(
            color = Color(0xFFFFFF00),
            cornerRadius = ConduitTheme.Dimensions.MessageBubble.CornerRadius
        ) {
            if (textInProgress != null && text.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
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
    color: Color,
    cornerRadius: Dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 2.dp,
                    color = Color.Black.copy(alpha = 0.25f),
                    offset = DpOffset(0.dp, 2.dp)
                )
            )
            .clip(shape)
            .background(
                color = color.copy(alpha = .5f),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = shape
            )
            .padding(8.dp)
    ) {
        content()
    }
}

//@Composable
//private fun MessageBubbleSurface(
//    color: Color,
//    cornerRadius: Dp,
//    content: @Composable () -> Unit
//) {
//    val shape = RoundedCornerShape(cornerRadius)
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .shadow(
//                elevation = 4.dp,
//                shape = shape
//            )
//            .background(
//                color = color.copy(alpha = .5f),
//                shape = shape
//            )
//            .border(
//                width = 1.dp,
//                color = Color.LightGray.copy(alpha = 1f),
//                shape = shape
//            )
//            .padding(6.dp)
//    ) {
//        content()
//    }
//}

// ---------------------------------------------------------------------------------
// Below likely deprecated

@Composable
private fun SystemMessageBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(fontSize = 12.sp, color = Color.Gray),
            modifier = Modifier.padding(10.dp)
        )
    }
}
