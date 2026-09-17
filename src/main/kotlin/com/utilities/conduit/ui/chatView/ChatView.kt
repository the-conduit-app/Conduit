package com.utilities.conduit.ui.chatView

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.Chat
import com.utilities.conduit.chat.Node
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.ui.LocalActions
import com.utilities.conduit.ui.PulsingImage
import com.utilities.conduit.ui.Sounds
import com.utilities.conduit.ui.SubtleScrollbar
import com.utilities.conduit.utils.AppUtils
import com.utilities.conduit.utils.ChatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ColumnScope.ChatView(appState: AppState) {
    val version = appState.chatManager.version // DO NOT REMOVE - recomp trigger
    val chat = appState.chatManager.currentChat
    val scrollState = rememberScrollState()
    val appActions = LocalActions.current
    val bringIntoViewRequesters = remember { mutableStateMapOf<String, BringIntoViewRequester>() }
    var highlightedNodeId by remember { mutableStateOf<String?>(null) }

    // The UI displays the single path from root to the current cursor.
    val historyNodes = ChatUtils.getFullHistory(chat, chat.cursorNodeId)

    //--------------------------------------------------------------------------
    // Branch-transition diagnostics

    val previousHistoryNodes = remember { mutableStateOf<List<Node>?>(null) }
    var branchTransition by remember { mutableStateOf<BranchTransition?>(null) }

    LaunchedEffect(historyNodes) {
        val oldPath = previousHistoryNodes.value

        if (oldPath != null && oldPath.map { it.id } != historyNodes.map { it.id }) {
            branchTransition = getBranchTransition(oldPath, historyNodes)
        }

        previousHistoryNodes.value = historyNodes
    }

    // Auto-scroll to bottom on addNode only (not branch switching etc.)
    LaunchedEffect(appState.chatManager.chatVersion) {
        if (historyNodes.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(appActions.scrollChatToNodeRequest) {
        val nodeId = appActions.scrollChatToNodeRequest ?: return@LaunchedEffect

        val requester = bringIntoViewRequesters[nodeId] ?: return@LaunchedEffect
        requester.bringIntoView()

        highlightedNodeId = nodeId
        delay(1000.milliseconds)
        if (highlightedNodeId == nodeId) {
            highlightedNodeId = null
        }
        appActions.clearScrollChatToNodeRequest()
    }

    val lastNode = historyNodes.lastOrNull()
    val generatingText = lastNode?.let { appState.chatManager.getTextInProgress(it.id) }
    LaunchedEffect(generatingText) {
        if (generatingText != null) { scrollState.scrollTo(scrollState.maxValue) }
    }

    //--------------------------------------------------------------------------

    // Title above message bubbles
    Text(
        text = "${chat.title} (${AppUtils.formatDateRange(chat.createdAt, System.currentTimeMillis())})",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    CompositionLocalProvider(
        LocalContextMenuRepresentation provides ConduitContextMenuRepresentation
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .padding(bottom = 10.dp)
            ) {
                BranchAnimatedHistory(
                    state = appState,
                    chat = chat,
                    historyNodes = historyNodes,
                    transition = branchTransition,
                    version = version,
                    highlightedNodeId = highlightedNodeId,
                    onNodeRequester = { nodeId, requester -> bringIntoViewRequesters[nodeId] = requester }
                )
            }
            SubtleScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

// Used for Branching animation ------------------------------------------

private data class BranchTransition(
    val shared: List<Node>,
    val outgoing: List<Node>,
    val incoming: List<Node>
)

private fun getBranchTransition(
    oldPath: List<Node>,
    newPath: List<Node>
): BranchTransition {
    var sharedCount = 0
    val maxShared = minOf(oldPath.size, newPath.size)

    while (
        sharedCount < maxShared &&
        oldPath[sharedCount].id == newPath[sharedCount].id
    ) {
        sharedCount++
    }

    return BranchTransition(
        shared = newPath.take(sharedCount),
        outgoing = oldPath.drop(sharedCount),
        incoming = newPath.drop(sharedCount)
    )
}

@Composable
private fun BranchAnimatedHistory(
    state: AppState,
    chat: Chat,
    historyNodes: List<Node>,
    transition: BranchTransition?,
    version: Int,
    highlightedNodeId: String?,
    onNodeRequester: (String, BringIntoViewRequester) -> Unit
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
                    onRequester = { requester -> onNodeRequester(node.id, requester) }
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
                    onRequester = { requester -> onNodeRequester(node.id, requester) }
                )
            }
        }

        // The changing suffix will be animated here.
        AnimatedContent(
            targetState = transition.incoming,
            transitionSpec = { (
                    slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith (
                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                )
            },
            label = "branch-suffix") { incomingNodes ->
            Column {
                incomingNodes.forEach { node ->
                    ChatNodeRow(state, chat, node, version,
                        isHighlighted = node.id == highlightedNodeId,
                        onRequester = { requester -> onNodeRequester(node.id, requester) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------

@Composable
private fun ChatNodeRow(
    appState: AppState,
    chat: Chat,
    node: Node,
    version: Int, // Needed to trigger recompose
    onRequester: (BringIntoViewRequester) -> Unit,
    isHighlighted: Boolean
) {
    val scope = rememberCoroutineScope()
    val isCursor = node.id == chat.cursorNodeId
    val isBranchable = node.children.size > 1 || (isCursor && node.children.isNotEmpty())
    val isUserNode = node.message?.authorType == AuthorType.USER
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(bringIntoViewRequester) {
        onRequester(bringIntoViewRequester)
    }

    // Highlighting jiggles the message bubble (scale up and down/damped sinusoid)
    val highlightScale = remember { Animatable(1f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            val startTime = withFrameNanos { it }

            while (true) {
                val amplitude = 0.25f
                val frequency = 8f
                val damping = 5f
                val elapsed = (withFrameNanos { it } - startTime) / 1_000_000_000f
                val scale = 1f + amplitude * sin(2f * PI.toFloat() * frequency * elapsed) * exp(-damping * elapsed)

                highlightScale.snapTo(scale)

                if (elapsed > 1f && abs(scale - 1f) < 0.001f)
                    break
            }
        }
        highlightScale.snapTo(1f)
    }

    val menuItems: (() -> List<ContextMenuItem>)? =
        if (isCursor && node.children.isEmpty()) {
            null
        } else {
            {
                // Re-eval to refresh at click time
                val itemIsCursor = node.id == chat.cursorNodeId
                val itemChildNodes = node.children.mapNotNull { childId -> chat.nodes[childId] }

                buildList {
                    if (!itemIsCursor) {
                        add(
                            ContextMenuItem("Start new branch from here or select one below") {
                                scope.launch { appState.chatManager.setCursor(node) }
                            }
                        )
                    }

                    itemChildNodes.forEach { node ->
                        val title = node.message?.title ?: "Untitled"
                        val isCurrentPath = ChatUtils.leadsToCursor(chat, node)

                        val text = node.message?.text
                            ?.replace(Regex("\\s+"), " ")
                            ?.trim()
                            ?.let { if (it.length > 50) "${it.take(50)}…" else it }
                            ?: "Empty"

                        val item = buildString {
                            append(if (isCurrentPath) "✓ " else "  ")
                            append(title)
                            append(" : ")
                            append(text)
                        }

                        add(
                            ContextMenuItem(item) {
                                scope.launch { appState.chatManager.selectBranch(node) }
                            }
                        )
                    }
                }
            }
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = highlightScale.value
                scaleY = highlightScale.value
            }
            .bringIntoViewRequester(bringIntoViewRequester),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isUserNode) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        // Branching user nodes (right aligned) have the branch cycling icon on their left
        if (isUserNode && isBranchable) {
            PulsingImage(
                modifier = Modifier.size(32.dp),
                onClick = {
                    Sounds.Swish.play()
                    Trace.log("Switching branches")
                    scope.launch {
                        appState.chatManager.cycleBranch(node)
                    }
                }
            )
        }

        val textInProgress = appState.chatManager.getTextInProgress(node.id)

        MessageBubble(
            node = node,
            textInProgress = textInProgress,
            isCursor = isCursor,
            contextMenuItems = menuItems
        )

        // Branching expert nodes (left aligned) have the branch cycling icon on their right
        if (!isUserNode && isBranchable) {
            PulsingImage(
                modifier = Modifier.size(20.dp),
                onClick = {
                    Sounds.Swish.play()
                    scope.launch {
                        appState.chatManager.cycleBranch(node)
                    }
                }
            )
        }
    }
}
