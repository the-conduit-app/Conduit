package com.utilities.conduit

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text2.input.TextFieldState.Saver.restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.utilities.conduit.ChatUtils.generateChatTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatsListView(state: AppState) {
    val chatsList = state.chatsList
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatsList.needsScrollingToTop) {
        if (chatsList.needsScrollingToTop) {
            listState.animateScrollToItem(0)
            chatsList.needsScrollingToTop = false
        }
    }

    val enabled = state.chatManager.currentChat.currentLeafNode?.message?.textInProgress?.value == null
    var showRenameDialog by remember { mutableStateOf<ChatsListItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (enabled) 1f else 0.25f)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            items(
                items = chatsList.items,
                key = { it.chat.id }
            ) { item ->
                ContextMenuArea(
                    items = {
                        listOf(
                            ContextMenuItem("Rename…") {
                                showRenameDialog = item
                            },
                            ContextMenuItem("Delete…") {
                                scope.launch {
                                    val removed = state.chatsList.remove(item)
                                    if (removed && item.chat.id == state.chatManager.currentChat.id) {
                                        state.notification.trigger(
                                            "Chat deleted. Continue chatting here to restore it."
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) {
                                state.chatManager.currentChat = item.chat
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.chat.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "(${formatDateRange(item.creationTime, item.modificationTime)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        showRenameDialog?.let { item ->
            ChatRenameDialog(
                initialTitle = item.chat.title,
                onCancel = { showRenameDialog = null },
                onSuggest = {
                        ChatUtils.generateChatTitle(state.systemExpert, item.chat)
                    },
                onOk = { newTitle ->
                    scope.launch {
                        state.chatsList.rename(item, newTitle)
                    }
                    showRenameDialog = null
                }
            )
        }
    }
}

private fun formatDateRange(
    creation: Long,
    modification: Long
): String {

    val created = Instant.ofEpochMilli(creation).atZone(ZoneId.systemDefault()).toLocalDate()
    val modified = Instant.ofEpochMilli(modification).atZone(ZoneId.systemDefault()).toLocalDate()

    val fmt = DateTimeFormatter.ofPattern("MMM d")

    return if (created == modified) {
        modified.format(fmt)
    } else {
        "${created.format(fmt)} – ${modified.format(fmt)}"
    }
}
