package com.utilities.conduit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.collections.get

@Composable
fun ColumnScope.ChatView(state: AppState) {
    val chat = state.chatManager.currentChat
    val historyNodes by remember(chat) {
        derivedStateOf { state.chatManager.getFullHistory(chat.currentLeafNodeId) }
    }
    val listState = rememberLazyListState()
    ////val lastStreamingText = historyNodes.lastOrNull()?.message?.textInProgress?.value
    LaunchedEffect(listState, historyNodes.size) { ->
        snapshotFlow {
            listState.layoutInfo.totalItemsCount to listState.layoutInfo.visibleItemsInfo.lastOrNull()?.size
        }.collect {
            if (historyNodes.isNotEmpty()) {
                listState.scrollToItem(historyNodes.lastIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 10.dp),
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFFF5F5F5))
    ) {
        items(historyNodes) { node -> MessageBubble(state, node = node) }
    }
}

@Composable
fun MessageBubble(state: AppState, node: Node) {
    val text = node.message?.textInProgress?.value ?: node.message?.text ?: ""

    val authorType = node.message?.author?.type ?: AuthorType.CONDUIT
    when (authorType) {
        AuthorType.USER -> UserMessageBubble(text)
        AuthorType.ASSISTANT -> ExpertMessageBubble(state, node, text)
        AuthorType.CONDUIT -> SystemMessageBubble(text)
    }
}

@Composable
private fun SystemMessageBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text.trim(),
            style = TextStyle(fontSize = 12.sp, color = Color.Gray),
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    val alignment = Alignment.End
    val bubbleColor = Color(0xFFDCF8C6) //// TODO: Move to theme
    val originator = "You"

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = alignment) {
        Text(text = originator, style = TextStyle(fontSize = 10.sp, color = Color.Gray))
        Text(
            text = text.trim(),
            modifier = Modifier.widthIn(max = 700.dp).background(bubbleColor, RoundedCornerShape(12.dp)).padding(10.dp)
        )
    }
}

@Composable
fun ExpertMessageBubble(state: AppState, node: Node, text: String) {
    val expertId = node.message?.author?.expertId

    val alignment = Alignment.Start
    val bubbleColor = Color(0xFFFFF9C4) // Light yellow
    val originator = state.expertsMap[expertId]?.nickname ?: "Expert"

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = alignment
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = originator, style = TextStyle(fontSize = 10.sp, color = Color.Gray))
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (state.currentExpert.value?.type == ExpertType.REMOTE) Icons.Default.Public else Icons.Default.Lock,
                contentDescription = "Lock - Local LLM, Globe - Remote LLM",
                tint = Color.Gray,
                modifier = Modifier.size(10.dp)
            )
        }

        if (text.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = text.trim(),
                modifier = Modifier.widthIn(max = 700.dp).background(bubbleColor, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            )
            if (node.message?.responseTime != null) {
                Text(
                    text = "Response took ${"%.1f".format(node.message.responseTime / 1000.0)}s",
                    style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                )
            }
        }
    }
}
