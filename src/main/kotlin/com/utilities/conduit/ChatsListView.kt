package com.utilities.conduit

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            itemsIndexed(
                items = chatsList.items,
                key = { _, item -> item.chat.id }
            ) { index, item ->
                val isCurrent = item.chat.id == state.chatManager.currentChat.id

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
                            .background(
                                if (isCurrent)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    Color.Transparent
                            )
                            .clickable(enabled = enabled) {
                                scope.launch {
                                    state.screenCurtain.show()
                                    state.chatManager.currentChat = item.chat
                                    state.chatsList.items[index] = item.copy(needsHumanReview = false)
                                    state.screenCurtain.hide()
                                    state.focusInput.value++ // just to trigger recomp of input area
                                }
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (item.needsHumanReview) FontWeight.SemiBold else FontWeight.Normal
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "(${AppUtils.formatDateRange(item.creationTime, item.modificationTime)})",
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
                        val updatedChat = state.chatsList.rename(item, newTitle)
                        if (updatedChat != null && updatedChat.id == state.chatManager.currentChat.id) {
                            state.chatManager.currentChat = updatedChat
                        }
                    }
                    showRenameDialog = null
                }
            )
        }
    }
}
