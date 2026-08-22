package com.utilities.conduit.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.Node

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(
    node: Node,
    isCursor: Boolean,
    isBranchable: Boolean = false,
    contextMenuItems: (() -> List<ContextMenuItem>)?
) {
    when (node.message?.author?.type ?: AuthorType.SYSTEM) {
        AuthorType.USER -> UserMessageBubble(node, isCursor, isBranchable, contextMenuItems)
        AuthorType.ASSISTANT -> ExpertMessageBubble(node, isCursor, isBranchable, contextMenuItems)
        AuthorType.SYSTEM -> SystemMessageBubble(node.message?.text ?: "Hmm... Wonder where this came from!")
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun UserMessageBubble(node: Node, isCursor: Boolean, isBranchable: Boolean, contextMenuItems: (() -> List<ContextMenuItem>)?) {
    val textInProgress = node.message?.textInProgress?.value
    val text = textInProgress ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donowatt" // an unknown amount of power
    var hasMore by remember { mutableStateOf(false) }

    val bubbleColor = if (isBranchable)
        ConduitTheme.Colors.MessageBubble.Branchable.User
    else
        ConduitTheme.Colors.MessageBubble.User

    val cornerRadius = if (isBranchable)
        ConduitTheme.Dimensions.MessageBubble.BranchableCornerRadius
    else
        ConduitTheme.Dimensions.MessageBubble.CornerRadius

    val shadowElevation = if (isBranchable)
        ConduitTheme.Dimensions.MessageBubble.ShadowElevation
    else
        0.dp

    val bubble: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ConduitTheme.Dimensions.MessageBubble.MaxWidth)
                .shadow(
                    elevation = shadowElevation,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .background(
                    bubbleColor,
                    RoundedCornerShape(cornerRadius)
                )
                .padding(ConduitTheme.Dimensions.MessageBubble.Padding),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = text.trim(),
                fontSize = if (isBranchable) 15.sp else 14.sp,
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
private fun ExpertMessageBubble(node: Node, isCursor: Boolean, isBranchable: Boolean, contextMenuItems: (() -> List<ContextMenuItem>)?) {
    val textInProgress = node.message?.textInProgress?.value
    val text = textInProgress ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donovich"
    var hasMore by remember { mutableStateOf(false) }

    val bubbleColor = if (isBranchable)
        ConduitTheme.Colors.MessageBubble.Branchable.Expert
    else
        ConduitTheme.Colors.MessageBubble.Expert

    val cornerRadius = if (isBranchable)
        ConduitTheme.Dimensions.MessageBubble.BranchableCornerRadius
    else
        ConduitTheme.Dimensions.MessageBubble.CornerRadius

    val shadowElevation = if (isBranchable)
        ConduitTheme.Dimensions.MessageBubble.ShadowElevation
    else
        0.dp

    val bubble: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ConduitTheme.Dimensions.MessageBubble.MaxWidth)
                .shadow(
                    elevation = shadowElevation,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .background(
                    bubbleColor,
                    RoundedCornerShape(cornerRadius)
                )
                .padding(10.dp)
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
                )  {
                    Text(
                        text = text.trim(),
                        fontSize = if (isBranchable) 15.sp else 14.sp,
                        maxLines = if (textInProgress == null && !isCursor) 5 else Int.MAX_VALUE,
                        onTextLayout = { result ->
                            hasMore = textInProgress == null && result.hasVisualOverflow
                        }
                    )

                    if (hasMore && !isCursor) {
                        val appActions = LocalActions.current
                        //Trace.log("Expert More: reading GlassHostController")
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
private fun SystemMessageBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(fontSize = 12.sp, color = Color.Gray),
            modifier = Modifier.padding(10.dp)
        )
    }
}
