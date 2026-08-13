package com.utilities.conduit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondary
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(state: AppState, node: Node, childCount: Int, isBranchPoint: Boolean = false) {
    val scope = rememberCoroutineScope()
    var showBranchingDialog by remember { mutableStateOf(false) }

    val bubbleColor = when {
        isBranchPoint && node.message?.author?.type == AuthorType.USER -> Color(0xFFF3D6DC) // Rosish
        isBranchPoint && node.message?.author?.type == AuthorType.ASSISTANT -> Color(0xFFFFE8B0) // Goldish
        node.message?.author?.type == AuthorType.USER -> Color(0xFFDCF8C6) // User - greenish
        else -> Color(0xFFFFF9C4) // Assistant - yellowish
    }

    val onBranchArrowClick = {
        if (node.children.size <= 2) {
            scope.launch { state.chatManager.selectOtherBranch(node) }
            showBranchingDialog = false
            Trace.log("BRANCH REVERT: cursor=${state.chatManager.currentChat.cursorNodeId}, version=${state.chatManager.version}")
        } else {
            showBranchingDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onPointerEvent(PointerEventType.Press) {
                if (it.button?.isSecondary == true) {
                    showBranchingDialog = true
                }
            }.onGloballyPositioned {
                bubblePosition = it.positionInWindow()
            }
    ) {
        when (node.message?.author?.type ?: AuthorType.SYSTEM) {
            AuthorType.USER -> UserMessageBubble(
                state = state,
                node = node,
                bubbleColor = bubbleColor,
                isBranchPoint = isBranchPoint,
                onBranchArrowClick = onBranchArrowClick
            )

            AuthorType.ASSISTANT -> ExpertMessageBubble(
                state = state,
                node = node,
                bubbleColor = bubbleColor,
                isBranchPoint = isBranchPoint,
                onBranchArrowClick = onBranchArrowClick
            )

            AuthorType.SYSTEM ->
                SystemMessageBubble(
                    node.message?.text ?: "Hmm... Wonder where this came from!"
                )
        }
    }

    //Trace.log("showBranchingDialog = ${showBranchingDialog}")
    // Branching dialog -------------------------------------------------------
    if (node.children.isNotEmpty() && showBranchingDialog) {
       Trace.log("DIALOG STATE node=${node.id} children=${node.children.size}")
        BranchingPopup(
            state = state,
            node = node,
            onDismiss = {
                showBranchingDialog = false
            },
            onSelectBranch = { selectedNode ->
                scope.launch { state.chatManager.selectBranch(selectedNode) }
                showBranchingDialog = false
            }
        )
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun UserMessageBubble(state: AppState, node: Node, bubbleColor: Color,
                              isBranchPoint: Boolean, onBranchArrowClick: () -> Unit = {})
{
    val text = node.message?.textInProgress?.value?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donowatt" // Could consume unknown amt of energy
    val alignment = Alignment.End

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment)
    {
        Text(text = title, style = TextStyle(fontSize = 10.sp, color = Color.Gray))

        Box {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBranchPoint) {
                    PulsingBranchIcon(
                        modifier = Modifier.size(22.dp),
                        onClick = onBranchArrowClick
                    )
                }
                Text(
                    text = text.trim(),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .widthIn(max = 704.dp)
                        .background(bubbleColor, RoundedCornerShape(12.dp))
                        .padding(11.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExpertMessageBubble(state: AppState, node: Node, bubbleColor: Color,
                        isBranchPoint: Boolean, onBranchArrowClick: () -> Unit)
{
    val text = node.message?.textInProgress?.value ?: node.message?.text.orEmpty()
    val title = node.message?.title ?: "Donovich"

    val alignment = Alignment.Start

    val isCurrentCursor = state.chatManager.currentChat.cursorNodeId == node.id
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = TextStyle(fontSize = 10.sp, color = Color.Gray)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = if (state.currentExpert.value?.type == ExpertType.REMOTE)
                    Icons.Default.Public else Icons.Default.Lock,
                contentDescription = "Lock - Local LLM, Globe - Remote LLM",
                tint = Color.Gray,
                modifier = Modifier.size(10.dp)
            )
        }

        if (text.isEmpty() && isCurrentCursor && node.message?.textInProgress?.value != null) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text.trim(),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .widthIn(max = 700.dp)
                        .background(
                            bubbleColor,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                )

                if (isBranchPoint) {
                    PulsingBranchIcon(
                        modifier = Modifier.size(22.dp),
                        onClick = onBranchArrowClick
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
