package com.utilities.conduit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.dk.kuiver.RelayoutPolicy
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.edge
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverInteractionCallbacks
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.renderer.KuiverViewerConfig
import com.utilities.conduit.AppState
import com.utilities.conduit.ui.treeView.ConduitTreeEdge
import com.utilities.conduit.ui.treeView.ConduitTreeNode
import com.utilities.conduit.ui.treeView.hierarchical
import kotlinx.coroutines.NonCancellable.isActive
import java.awt.SystemColor.text
import kotlin.math.roundToInt

@Composable
fun ChatTreeView(
    state: AppState
) {
    val chat = state.chatManager.currentChat
    val nodeAddedVersion = state.chatManager.nodeAddedVersion
    val messagePanel = LocalMessagePanel.current

    val kuiver = remember(chat, state.chatManager.nodeAddedVersion) {
        buildKuiver {
            chat.nodes.values.forEach { node ->
                nodes(node.id)
            }

            chat.nodes.values.forEach { node ->
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

    Box(modifier = Modifier.fillMaxSize()) {

        KuiverViewer(
            state = kuiverState,
            modifier = Modifier.fillMaxSize(),

            config = KuiverViewerConfig(
                nodeDragEnabled = true,
                relayoutPolicy = RelayoutPolicy.KEEP_MANUAL,
                fitToContent = false
            ),

            callbacks = KuiverInteractionCallbacks(
                onNodeDragStart = { },
                onNodeDragEnd = { _, _ -> }
            ),

            nodeContent = { kuiverNode ->
                val conduitNode = chat.nodes[kuiverNode.id]
                val isActive = conduitNode?.message?.textInProgress?.value != null

                if (conduitNode != null) {
                    ConduitTreeNode(
                        kuiverNode = kuiverNode,
                        isCursor = kuiverNode.id == cursorNodeId,
                        isActive = isActive,

                        onHoverChanged = { hovered, position ->
                            if (hovered) {
                                messagePanel.show(
                                    text = conduitNode.message?.text ?: "",
                                    position = position
                                )
                            } else {
                                messagePanel.hide()
                            }
                        },

                        onClick = {
                            // cursorNodeId = kuiverNode.id // Testing
                        }
                    )
                }
            },

            edgeContent = { _, start, end ->
                ConduitTreeEdge(
                    start = start,
                    end = end
                )
            }
        )

    }
}
