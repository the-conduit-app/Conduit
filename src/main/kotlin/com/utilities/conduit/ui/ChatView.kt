package com.utilities.conduit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.AppUtils
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.Chat
import com.utilities.conduit.chat.ChatUtils
import com.utilities.conduit.chat.Node
import com.utilities.conduit.debug.Trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.let
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

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
    onNodePositioned: (String, Int) -> Unit
) {
    if (transition == null) {
        Column {
            historyNodes.forEach { node ->
                ChatNodeRow(
                    state = state,
                    chat = chat,
                    node = node,
                    version = version,
                    isHighlighted = node.id == highlightedNodeId,
                    onPositioned = { y ->
                        onNodePositioned(node.id, y)
                    }
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
                    onPositioned = { y -> onNodePositioned(node.id, y) }
                )
            }
        }

        // The changing suffix will be animated here.
        AnimatedContent(
            targetState = transition.incoming,
            transitionSpec = {
                (
                        slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith (
                        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                )
            },
            label = "branch-suffix") { incomingNodes ->
            Column {
                incomingNodes.forEach {
                    node -> ChatNodeRow(state, chat, node, version,
                        isHighlighted = node.id == highlightedNodeId,
                        onPositioned = { y -> onNodePositioned(node.id, y) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------

@Composable
fun ColumnScope.ChatView(state: AppState) {
    val version = state.chatManager.version // DO NOT REMOVE - recomp trigger
    val chat = state.chatManager.currentChat
    val scrollState = rememberScrollState()
    val appActions = LocalActions.current

    val nodePositions = remember { mutableStateMapOf<String, Int>() }
    val onNodePositioned: (String, Int) -> Unit = { nodeId, y ->
        nodePositions[nodeId] = y
    }
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
    LaunchedEffect(state.chatManager.nodeAddedVersion) {
        if (historyNodes.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(appActions.scrollChatToNodeRequest) {
        val nodeId = appActions.scrollChatToNodeRequest ?: return@LaunchedEffect
        val nodeY = nodePositions[nodeId] ?: return@LaunchedEffect
        Trace.log("TREE SCROLL: node=$nodeId y=$nodeY")

        val viewportHeight = scrollState.viewportSize
        val target = nodeY - viewportHeight / 2

        scrollState.animateScrollTo(
            target.coerceIn(0, scrollState.maxValue)
        )

        // Briefly animate the scrolled node to help identify
        highlightedNodeId = nodeId
        delay(1000.milliseconds)
        if (highlightedNodeId == nodeId) { highlightedNodeId = null }
    }

    val generatingMessage = historyNodes.lastOrNull()?.message?.takeIf { it.textInProgress.value != null }
    LaunchedEffect(generatingMessage?.textInProgress?.value) {
        if (generatingMessage != null) {
            scrollState.scrollTo(scrollState.maxValue)
        }
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
        LocalContextMenuRepresentation provides ChatMessageContextMenuRepresentation
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
                    state = state,
                    chat = chat,
                    historyNodes = historyNodes,
                    transition = branchTransition,
                    version = version,
                    highlightedNodeId,
                    onNodePositioned = onNodePositioned
                )
            }

            if (state.rightScreenCurtain.isActive) {
                val screenColor = Color(0xFFF2F2F2)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .matchParentSize()
                        .alpha(state.rightScreenCurtain.opacity)
                        .background(screenColor)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun ChatNodeRow(
    state: AppState,
    chat: Chat,
    node: Node,
    version: Int,
    onPositioned: (Int) -> Unit,
    isHighlighted: Boolean
) {
    val scope = rememberCoroutineScope()
    val isCursor = node.id == chat.cursorNodeId
    val isBranchable = node.children.size > 1 || (isCursor && node.children.isNotEmpty())
    val isUserNode = node.message?.author?.type == AuthorType.USER

    // Highlighting draws attention to a message bubble (scale up and down/damped sinusoid)
    val highlightScale = remember { Animatable(1f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            val startTime = withFrameNanos { it }

            while (true) {
                val amplitude = 0.25f
                val frequency = 8f
                val damping = 5f
                val elapsed = (withFrameNanos { it } - startTime) / 1_000_000_000f
                val scale = 1f + amplitude *
                            sin(2f * PI.toFloat() * frequency * elapsed) *
                            exp(-damping * elapsed)

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
                                scope.launch { state.chatManager.setCursor(node) }
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
                                scope.launch { state.chatManager.selectBranch(node) }
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
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.positionInParent().y.roundToInt()
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isUserNode) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        // Branching user nodes (right aligned) have the branch cycling icon on their left
        if (isUserNode && isBranchable) {
            PulsingBranchIcon(
                modifier = Modifier.size(32.dp),
                onClick = {
                    scope.launch {
                        state.chatManager.cycleBranch(node)
                    }
                }
            )
        }

        MessageBubble(
            node = node,
            isCursor = isCursor,
            isBranchable = isBranchable,
            contextMenuItems = menuItems
        )

        // Branching expert nodes (left aligned) have the branch cycling icon on their right
        if (!isUserNode && isBranchable) {
            PulsingBranchIcon(
                modifier = Modifier.size(20.dp),
                onClick = {
                    scope.launch {
                        state.chatManager.cycleBranch(node)
                    }
                }
            )
        }
    }
}
