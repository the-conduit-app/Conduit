package com.utilities.conduit.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.AppUtils
import com.utilities.conduit.chat.ChatUtils
import com.utilities.conduit.chat.ChatsListItem
import com.utilities.conduit.ui.sounds.Tick
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@Composable
fun ChatsListView(state: AppState) {
    val chatsList = state.chatsList
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(chatsList.needsScrollingToTop) {
        if (chatsList.needsScrollingToTop) {
            listState.animateScrollToItem(0)
            chatsList.needsScrollingToTop = false
        }
    }
    LaunchedEffect(Unit) {
        yield() // why? hack?
        focusRequester.requestFocus()
    }

    val enabled = !state.chatManager.isGenerating
    var showRenameDialog by remember { mutableStateOf<ChatsListItem?>(null) }

    suspend fun selectChat(item: ChatsListItem) {
        if (item.chat.id == state.chatManager.currentChat.id) return
        Tick.play()

        state.rightScreenCurtain.show()
        state.chatManager.currentChat = item.chat
        if (item.chat.needsHumanReview)
            chatsList.setNeedsHumanReview(item.chat.id, false)
        state.rightScreenCurtain.hide()
        state.focusInput.value++ // Trigger recomp
    }

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
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown)
                        return@onPreviewKeyEvent false

                    val direction = when (event.key) {
                        Key.DirectionUp -> -1
                        Key.DirectionDown -> 1
                        else -> return@onPreviewKeyEvent false
                    }

                    scope.launch {
                        val currentIndex = chatsList.items.indexOfFirst {
                            it.chat.id == state.chatManager.currentChat.id
                        }
                        val newIndex = currentIndex + direction

                        if (currentIndex >= 0 && newIndex in chatsList.items.indices) {
                            val item = chatsList.items[newIndex]
                            selectChat(item)
                        }
                    }
                    true
                }
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
                            },
                            ContextMenuItem("TreeView") {
                                scope.launch {
                                    selectChat(item)
                                    state.leftPanelMode = LeftPanelMode.TREE
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
                                    selectChat(item)
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
                            fontWeight = if (item.chat.needsHumanReview) FontWeight.SemiBold else FontWeight.Normal
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
                            //state.chatManager.currentChat = updatedChat
                            selectChat(item.copy(chat = updatedChat))
                        }
                    }
                    showRenameDialog = null
                }
            )
        }
    }
}
