package com.utilities.conduit

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.NonCancellable.children
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(
    node: Node,
    isBranchPoint: Boolean = false,
    contextMenuItems: () -> List<ContextMenuItem>
) {
    val bubbleColor = when {
        isBranchPoint && node.message?.author?.type == AuthorType.USER -> Color(0xFFF3D6DC)
        isBranchPoint && node.message?.author?.type == AuthorType.ASSISTANT -> Color(0xFFFFE8B0)
        node.message?.author?.type == AuthorType.USER -> Color(0xFFDCF8C6)
        else -> Color(0xFFFFF9C4)
    }

    when (node.message?.author?.type ?: AuthorType.SYSTEM) {
        AuthorType.USER -> UserMessageBubble(node = node, bubbleColor = bubbleColor, contextMenuItems = contextMenuItems)
        AuthorType.ASSISTANT -> ExpertMessageBubble(node = node, bubbleColor = bubbleColor, contextMenuItems = contextMenuItems)
        AuthorType.SYSTEM -> SystemMessageBubble(node.message?.text ?: "Hmm... Wonder where this came from!")
    }
}


// ----------------------------------------------------------------------------------------

@Composable
private fun UserMessageBubble(node: Node, bubbleColor: Color, contextMenuItems: () -> List<ContextMenuItem>) {
    val text = node.message?.textInProgress?.value ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donowatt"

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

        ContextMenuArea(items = contextMenuItems) {
            Text(
                text = text.trim(),
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .background(
                        bubbleColor,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(11.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun ExpertMessageBubble(
    node: Node,
    bubbleColor: Color,
    contextMenuItems: () -> List<ContextMenuItem>
) {
    val textInProgress = node.message?.textInProgress?.value
    val text = textInProgress ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donovich"

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

        ContextMenuArea(
            items = contextMenuItems
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .background(
                        bubbleColor,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
            ) {
                if (textInProgress != null && text.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = text.trim(),
                        fontSize = 14.sp
                    )
                }
            }
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
private fun SystemMessageBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(fontSize = 12.sp, color = Color.Gray),
            modifier = Modifier.padding(10.dp)
        )
    }
}
