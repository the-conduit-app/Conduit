package com.utilities.conduit.ui.chatView

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.utils.AppUtils
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.Chat
import com.utilities.conduit.utils.ChatUtils
import com.utilities.conduit.chat.Node
import com.utilities.conduit.ui.LocalActions
import com.utilities.conduit.ui.PulsingImage
import com.utilities.conduit.ui.Sounds
import com.utilities.conduit.ui.SubtleScrollbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.collections.forEach
import kotlin.let
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ColumnScope.ChatView(appState: AppState) {
    val version = appState.chatManager.version // DO NOT REMOVE - recomp trigger
    val chat = appState.chatManager.currentChat
    val scrollState = rememberScrollState()
    val appActions = LocalActions.current

    val nodePositions = remember { mutableStateMapOf<String, Int>() }
    var chatColumnCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val onNodePositioned: (String, Int) -> Unit = { nodeId, y ->
        nodePositions[nodeId] = y
    }

    // Used to jiggle a node when corresponding node in TreeView is clicked
    var highlightedNodeId by remember { mutableStateOf<String?>(null) }

    // ChatView shows the single path from root to the current cursor. TreeView shows other paths dimmed
    val historyNodes = ChatUtils.getFullHistory(chat, chat.cursorNodeId)

    //--------------------------------------------------------------------------

    val previousHistoryNodes = remember { mutableStateOf<List<Node>?>(null) }
    var branchTransition by remember { mutableStateOf<BranchTransition?>(null) }

    // Trigger branching effect on effective cursor change.
    LaunchedEffect(historyNodes) {
        val oldHistory = previousHistoryNodes.value
        val newHistory = historyNodes

        if (oldHistory != null && oldHistory.map { it.id } != newHistory.map { it.id }) {
            branchTransition = getBranchTransition(oldHistory, newHistory)
        }
        previousHistoryNodes.value = newHistory
    }

    // Auto-scroll to bottom on addNode only (not branch switching etc.)
    LaunchedEffect(appState.chatManager.chatVersion) {
        if (historyNodes.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Scrolling triggered by TreeView node click
    LaunchedEffect(appActions.scrollChatToNodeRequest) {
        val nodeId = appActions.scrollChatToNodeRequest ?: return@LaunchedEffect

        // Wait until the requested node has been composed.
        snapshotFlow { nodePositions[nodeId] }.filterNotNull().first()
        val nodeY = nodePositions[nodeId] ?: return@LaunchedEffect
        val target = nodeY - scrollState.viewportSize / 2
        scrollState.animateScrollTo(target.coerceIn(0, scrollState.maxValue))

        // Briefly animate the scrolled node in ChatView to help identify
        highlightedNodeId = nodeId
        delay(1000.milliseconds)
        if (highlightedNodeId == nodeId) { highlightedNodeId = null }
        appActions.clearScrollChatToNodeRequest()
    }

    LaunchedEffect(appState.chatManager.currentlyGeneratingNodeId) {
        val generatingNodeId = appState.chatManager.currentlyGeneratingNodeId
            ?: return@LaunchedEffect

        snapshotFlow {
            appState.chatManager.getTextInProgress(generatingNodeId)
        }.collect { text ->
            if (text != null) {
                println("TEXT CHANGED: max=${scrollState.maxValue}, value=${scrollState.value}")
                scrollState.scrollTo(scrollState.maxValue)
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            scrollState.maxValue
        }.collect { maxValue ->
            println(
                "MAX CHANGED: max=$maxValue, value=${scrollState.value}, " +
                        "generating=${appState.chatManager.currentlyGeneratingNodeId}"
            )
        }
    }

//    // Scroll to the last node if it is actively generating text
//    val lastNode = historyNodes.lastOrNull()
//
//    val generatingText = lastNode?.let { appState.chatManager.getTextInProgress(it.id) }
//    LaunchedEffect(generatingText) {
//        if (generatingText != null) {
//            withFrameNanos { }
//            scrollState.scrollTo(scrollState.maxValue)
//        }
//    }
//    LaunchedEffect(generatingText) {
//        if (generatingText != null) {
//            println("BEFORE FRAME: max=${scrollState.maxValue}")
//            withFrameNanos { }
//            println("AFTER FRAME: max=${scrollState.maxValue}")
//            scrollState.scrollTo(scrollState.maxValue)
//            println("AFTER SCROLL: value=${scrollState.value}")
//        }
//    }
    //--------------------------------------------------------------------------
    // ChatView Panel

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
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .padding(bottom = 10.dp)
                    .onGloballyPositioned { coordinates ->
                        chatColumnCoordinates = coordinates
                    }
            ) {
                // This will render the individual ChatRows in the chat view (def below)
                AnimatedBranchView(
                    state = appState,
                    chat = chat,
                    historyNodes = historyNodes,
                    transition = branchTransition,
                    version = version,
                    highlightedNodeId,
                    onNodePositioned = onNodePositioned,
                    chatColumnCoordinates = chatColumnCoordinates
                )
            }
            SubtleScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            ////
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        appState.scope.launch {
                            println("scroll to bot")
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                ) { Text("Scroll to Bottom") }
            }
        }
    }
}

// -------------------------------------------------------------------------

// A single row of ChatView - contains a message bubble and if a branching node
// the pulsing branch icon beside it.
@Composable
fun ChatNodeRow(
    appState: AppState,
    chat: Chat,
    node: Node,
    version: Int,
    onPositioned: (Int) -> Unit,
    isHighlighted: Boolean,
    chatColumnCoordinates: LayoutCoordinates?
) {
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val isCursor = node.id == chat.cursorNodeId
    val isBranchable = node.children.size > 1 || (isCursor && node.children.isNotEmpty())
    val isUserNode = node.message?.authorType == AuthorType.USER

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

    // Scrolls the last bubble into view on incremental change in response
    val textInProgress = appState.chatManager.getTextInProgress(node.id)
    var wasGenerating by remember { mutableStateOf(false) }
    LaunchedEffect(textInProgress) {
        if (textInProgress == null && wasGenerating) {
            wasGenerating = false
            delay(50.milliseconds)
            bringIntoViewRequester.bringIntoView()
        }
    }

    // This generates the context menu items for each ChatNodeRow (Start new branch
    // or select a child)
    val getMenuItems: (() -> List<ContextMenuItem>)? =
        if (isCursor && node.children.isEmpty()) {
            null
        } else {
            {
                // Re-eval to refresh at click time
                val itemIsCursor = node.id == chat.cursorNodeId
                val itemChildNodes = node.children.mapNotNull { childId -> chat.nodes[childId] }

                buildList {
                    if (!itemIsCursor) { // Can't start new branch from the cursor
                        add(
                            ContextMenuItem("Start new branch from here or select one below") {
                                Sounds.Swish.play()
                                scope.launch { appState.chatManager.setCursor(node) }
                            }
                        )
                    }

                    itemChildNodes.forEach { node ->
                        val title = node.message?.title ?: "Donovatt"
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
                                Sounds.Swish.play()
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
            .onGloballyPositioned { coordinates ->
                chatColumnCoordinates?.let { columnCoordinates ->
                    val y = columnCoordinates.localPositionOf(coordinates, Offset.Zero).y.roundToInt()
                    onPositioned(y)
                }
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
            PulsingImage(
                modifier = Modifier.size(32.dp),
                onClick = {
                    Sounds.Swish.play()
                    scope.launch { appState.chatManager.cycleBranch(node) }
                }
            )
        }

        val textInProgress = appState.chatManager.getTextInProgress(node.id)
        MessageBubble(
            modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
            node = node,
            textInProgress = textInProgress,
            isCursor = isCursor,
            contextMenuItems = getMenuItems,
        )

        // Branching expert nodes (left aligned) have the branch cycling icon on their right
        if (!isUserNode && isBranchable) {
            PulsingImage(
                modifier = Modifier.size(32.dp),
                onClick = {
                    Sounds.Swish.play()
                    scope.launch { appState.chatManager.cycleBranch(node) }
                }
            )
        }
    }
}
