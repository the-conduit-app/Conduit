package com.utilities.conduit.ui.chatView

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.layout.LayoutCoordinates
import com.utilities.conduit.AppState
import com.utilities.conduit.chat.Chat
import com.utilities.conduit.chat.Node
import kotlin.collections.forEach

// A Branch Transition object has incoming, outgoing and shared nodes.
// Nodes from root to some common branch point are shared between paths A and B.
// The remaining B nodes are animated in and the remaining A nodes are animated
// out with coolness by SwitchBranchWithAnimation() below
data class BranchTransition(
    val shared: List<Node>,
    val outgoing: List<Node>,
    val incoming: List<Node>
)
fun getBranchTransition(oldPath: List<Node>, newPath: List<Node>): BranchTransition {
    var sharedCount = 0
    val maxShared = minOf(oldPath.size, newPath.size)

    while (sharedCount < maxShared && oldPath[sharedCount].id == newPath[sharedCount].id) {
        sharedCount++
    }

    return BranchTransition(
        shared = newPath.take(sharedCount),
        outgoing = oldPath.drop(sharedCount),
        incoming = newPath.drop(sharedCount)
    )
}

@Composable
fun AnimatedBranchView(
    state: AppState,
    chat: Chat,
    historyNodes: List<Node>,
    transition: BranchTransition?,
    version: Int,
    highlightedNodeId: String?,
    onNodePositioned: (String, Int) -> Unit,
    chatColumnCoordinates: LayoutCoordinates?,
) {
    if (transition == null) {
        Column {
            historyNodes.forEach { node ->
                ChatNodeRow(
                    appState = state,
                    chat = chat,
                    node = node,
                    version = version,
                    isHighlighted = node.id == highlightedNodeId,
                    onPositioned = { y -> onNodePositioned(node.id, y) },
                    chatColumnCoordinates = chatColumnCoordinates,
                )
            }
        }
        return
    }

    Column {
        // Shared history remains stationary.
        transition.shared.forEach { node ->
            key(node.id, chat.cursorNodeId) {
                ChatNodeRow(state, chat, node, version,
                    isHighlighted = node.id == highlightedNodeId,
                    onPositioned = { y -> onNodePositioned(node.id, y) },
                    chatColumnCoordinates = chatColumnCoordinates,
                )
            }
        }

        AnimatedContent(
            // Animate in/out the changing suffix
            targetState = transition.incoming,
            transitionSpec = {
                (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith (
                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                )
            },

            // Render the new nodes
            label = "branch-suffix") { incomingNodes ->
            Column {
                incomingNodes.forEach { node ->
                    ChatNodeRow(state, chat, node, version,
                        isHighlighted = node.id == highlightedNodeId,
                        onPositioned = { y -> onNodePositioned(node.id, y) },
                        chatColumnCoordinates = chatColumnCoordinates,
                    )
                }
            }
        }
    }
}
