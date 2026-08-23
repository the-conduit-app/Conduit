package com.utilities.conduit.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dk.kuiver.RelayoutPolicy
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.edge
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverInteractionCallbacks
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.renderer.KuiverViewerConfig
import com.dk.kuiver.ui.EdgeContent
import com.dk.kuiver.ui.EdgeShape
import com.dk.kuiver.ui.EdgeStyle
import com.utilities.conduit.AppState
import com.utilities.conduit.ui.treeView.ConduitTreeEdge
import com.utilities.conduit.ui.treeView.ConduitTreeNode
import com.utilities.conduit.ui.treeView.hierarchical
import com.dk.kuiver.ui.StyledEdgeContent

@Composable
fun ChatTreeView(state: AppState) {

    val chat = state.chatManager.currentChat

    val kuiver = remember(chat) {
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

    val kuiverState = rememberKuiverViewerState(
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

    var cursorNodeId by remember {
        mutableStateOf(chat.cursorNodeId)
    }

    KuiverViewer(
        state = kuiverState,
        modifier = Modifier.fillMaxSize(),

        config = KuiverViewerConfig(
            nodeDragEnabled = true,
            relayoutPolicy = RelayoutPolicy.KEEP_MANUAL,
            fitToContent = false
        ),

        callbacks = KuiverInteractionCallbacks(
            onNodeDragStart = { node ->
                //println("Drag started: ${node.id}")
            },
            onNodeDragEnd = { node, offset ->
                //println("Drag ended: ${node.id}, moved by $offset")
            }
        ),

        nodeContent = { kuiverNode ->
            val conduitNode = chat.nodes[kuiverNode.id]

            if (conduitNode != null) {
                ConduitTreeNode(
                    node = kuiverNode,
                    isCursor = kuiverNode.id == cursorNodeId,
                    onClick = {
                        cursorNodeId = kuiverNode.id
                        ////println("Node clicked: ${node.id}")
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
