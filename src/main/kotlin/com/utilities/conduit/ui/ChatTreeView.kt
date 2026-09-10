package com.utilities.conduit.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dk.kuiver.RelayoutPolicy
import com.dk.kuiver.model.KuiverEdge
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.edge
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverInteractionCallbacks
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.renderer.KuiverViewerConfig
import com.utilities.conduit.AppState
import com.utilities.conduit.chat.Chat
import com.utilities.conduit.chat.Node
import com.utilities.conduit.ui.treeView.ConduitTreeEdge
import com.utilities.conduit.ui.treeView.ConduitTreeNode
import com.utilities.conduit.ui.treeView.hierarchical
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset

@Composable
fun ChatTreeView(
    state: AppState
) {
    val messagePanel = LocalMessagePanelController.current
    val appActions = LocalActions.current
    val scope = rememberCoroutineScope()

    val chat = state.chatManager.currentChat
    val nodesOnCursorPath = activePathIds(chat)
    val nodeAddedVersion = state.chatManager.nodeAddedVersion

    val kuiver = remember(chat, state.chatManager.nodeAddedVersion) {
        val nodes = chat.nodes.values.toList()
        buildKuiver {
            nodes.forEach { node ->
                nodes(node.id)
            }

            nodes.forEach { node ->
                node.children.forEach { childId ->
                    edge(node.id, childId)
                }
            }
        }
    }

    val kuiverState = key(chat, nodeAddedVersion) {
        rememberKuiverViewerState(
            initialKuiver = kuiver,
            LayoutConfig.Custom(
                provider = { kuiver, config ->
                    hierarchical(
                        kuiver = kuiver,
                        width = config.width,
                        height = config.height,
                        nodeSpacing = 25.dp,
                        levelSpacing = 50.dp
                    )
                }
            )
        )
    }

    var cursorNodeId by remember(chat, state.chatManager.version) {
        mutableStateOf(chat.cursorNodeId)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .innerShadow(
                    shape = RoundedCornerShape(8.dp),
                    shadow = Shadow(
                        radius = 8.dp,
                        spread = 0.dp,
                        offset = DpOffset.Zero,
                        color = Color.Black.copy(alpha = 0.25f)
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color(0xFF007C91).copy(alpha=0.25f), // Color.Magenta.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(4.dp)
        ) {
            KuiverViewer(
                state = kuiverState,
                modifier = Modifier.fillMaxSize(),

                config = KuiverViewerConfig(
                    nodeDragEnabled = true,
                    relayoutPolicy = RelayoutPolicy.KEEP_MANUAL,
                    fitToContent = false,
                    zoomConditionDesktop = { true }
                ),

                callbacks = KuiverInteractionCallbacks(
                    onNodeDragStart = { },
                    onNodeDragEnd = { _, _ -> }
                ),

                nodeContent = { kuiverNode ->
                    val conduitNode = chat.nodes[kuiverNode.id]
                    val isActive = conduitNode?.message?.textInProgress?.value != null

                    if (conduitNode != null) {

                        @Composable
                        fun renderNode() {
                            ConduitTreeNode(
                                //kuiverNode = kuiverNode,
                                isCursor = kuiverNode.id == cursorNodeId,
                                isActive = isActive,
                                leadsToCursor = nodesOnCursorPath.contains(conduitNode.id),
                                onHoverChanged = { hovered, position ->
                                    if (hovered) {
                                        messagePanel.show(
                                            title = conduitNode.message?.title ?: "",
                                            text = conduitNode.message?.text ?: "",
                                            position = position.copy(y = position.y + 25f)
                                        )
                                    } else {
                                        messagePanel.hide()
                                    }
                                },
                                onClick = {
                                    if (nodesOnCursorPath.contains(conduitNode.id)) {
                                        appActions.scrollChatToNode(conduitNode.id)
                                        Sounds.Drop.play()
                                    }
                                }
                            )
                        }

                        if (nodesOnCursorPath.contains(conduitNode.id)) {
                            renderNode()
                        } else {
                            CompositionLocalProvider(
                                LocalContextMenuRepresentation provides ConduitContextMenuRepresentation
                            ) {
                                ContextMenuArea(
                                    items = {
                                        listOf(
                                            ContextMenuItem("Teleport to here?") {
                                                messagePanel.hide()
                                                scope.launch {
                                                    Sounds.Teleport.play()
                                                    state.chatManager.setCursor(conduitNode)
                                                }
                                            }
                                        )
                                    }
                                ) {
                                    renderNode()
                                }
                            }
                        }
                    }
                },

                edgeContent = { edge: KuiverEdge, start, end ->
                    ConduitTreeEdge(
                        start = start,
                        end = end,
                        leadsToCursor = (edge.fromId in nodesOnCursorPath && edge.toId in nodesOnCursorPath)
                    )
                }
            )
        }
    }
}

private fun activePathIds(chat: Chat): Set<String> {
    val path = mutableSetOf<String>()
    var id = chat.cursorNodeId

    while (id != null) {
        path += id
        id = chat.nodes[id]?.parentId
    }

    return path
}

// Makes a title like "You: Explain the Riemann..." or "Gem (Default pack): "..."
// although note that a root node necessarily has to originate from the user
// Use node.message.title: node.message.text.take(25)...
private fun makeNodeHeader(node: Node?): String {
    val title = node?.message?.title?.takeIf { it.isNotBlank() } ?: "Donohue"
    val firstLine = node?.message?.text
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?: "Donowatt"

    val message = if (firstLine.length > 25) {
        firstLine.take(25) + "..."
    } else {
        firstLine
    }

    return "$title: $message"
}
