package com.utilities.conduit

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.let

@Composable
fun ColumnScope.ChatView(state: AppState) {
    val version = state.chatManager.version // DO NOT REMOVE - recomp trigger
    val chat = state.chatManager.currentChat
    val listState = rememberLazyListState()

    // The UI displays the single path from root to the current cursor.
    val historyNodes = ChatUtils.getFullHistory(chat, chat.cursorNodeId)

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
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                val version = state.chatManager.version

                items(
                    items = historyNodes,
                    key = { node -> "${node.id}-$version" }
                ) { node ->
                    ChatNodeRow(
                        state = state,
                        chat = chat,
                        node = node,
                        version
                    )
                }
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

    // Auto-scroll to bottom on addNode only (not branch switching etc.)
    LaunchedEffect(state.chatManager.nodeAddedVersion) {
        if (historyNodes.isNotEmpty()) {
            listState.scrollToItem(historyNodes.lastIndex)
        }
    }
}

// ----------------------------------------------------------------------------------------

@Composable
private fun ChatNodeRow(state: AppState, chat: Chat, node: Node, version: Int) {
    val scope = rememberCoroutineScope()
    val isCursor = node.id == chat.cursorNodeId
    val isBranchable = node.children.size > 1 || (isCursor && node.children.isNotEmpty())
    val isUserNode = node.message?.author?.type == AuthorType.USER

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
        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.size(20.dp),
                onClick = {
                    scope.launch {
                        state.chatManager.cycleBranch(node)
                    }
                }
            )
        }

        MessageBubble(
            node = node,
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
