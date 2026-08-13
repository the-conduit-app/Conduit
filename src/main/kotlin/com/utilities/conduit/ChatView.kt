package com.utilities.conduit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.ChatView(state: AppState) {
    val version = state.chatManager.version // DO NOT REMOVE - recomp trigger
    val chatBackground = Color.Transparent
    val chat = state.chatManager.currentChat
    val listState = rememberLazyListState()
    val historyNodes = ChatUtils.getFullHistory(chat, chat.cursorNodeId)

    // title above message bubbles
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

    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(chatBackground)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(items = historyNodes, key = { node -> node.id }) { node ->
                val isBranchPoint = node.children.size > 1 || (node.id == chat.cursorNodeId && node.children.isNotEmpty())
                MessageBubble(state, node = node, childCount = node.children.size, isBranchPoint = isBranchPoint)
            }
        }
        if (state.rightScreenCurtain.isActive) {
            val screenColor = Color(0xFFF2F2F2) ////
            Box(modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .matchParentSize().alpha(state.rightScreenCurtain.opacity).background(screenColor)
            )
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(listState, historyNodes.size) { ->
        snapshotFlow {
            listState.layoutInfo.totalItemsCount to listState.layoutInfo.visibleItemsInfo.lastOrNull()?.size
        }.collect {
            if (historyNodes.isNotEmpty()) {
                listState.scrollToItem(historyNodes.lastIndex)
            }
        }
    }
}
