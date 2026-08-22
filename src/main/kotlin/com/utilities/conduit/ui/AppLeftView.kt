package com.utilities.conduit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import kotlinx.coroutines.launch

enum class LeftPanelMode {
    LIST,
    TREE
}

@Composable
fun AppLeftView(state: AppState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (state.leftPanelMode) {
                        LeftPanelMode.LIST -> "Chats"
                        LeftPanelMode.TREE -> state.chatManager.currentChat.title
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (state.leftPanelMode) {
                        LeftPanelMode.LIST -> {
                            // New chat ------------------------------------------------
                            Spacer(Modifier.width(6.dp))

                            IconButton(
                                modifier = Modifier.size(24.dp),
                                enabled = state.chatManager.currentlyGeneratingExpert == null,
                                onClick = {
                                    val chat = state.chatManager.createChat()
                                    state.focusInput.value++
                                    state.chatManager.currentChat = chat
                                }
                            ) {
                                Icon(
                                    imageVector = ConduitIcons.Add,
                                    contentDescription = "New chat"
                                )
                            }
                        }

                        LeftPanelMode.TREE -> {
                            // Return to chat list -------------------------------------
                            IconButton(
                                modifier = Modifier.size(24.dp),
                                onClick = {
                                    scope.launch {
                                        state.leftScreenCurtain.show()
                                        state.leftPanelMode = LeftPanelMode.LIST
                                        state.leftScreenCurtain.hide()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = ConduitIcons.Menu,
                                    contentDescription = "Show chat list",
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Body ---------------------------------------------------------------
            when (state.leftPanelMode) {
                LeftPanelMode.LIST -> ChatsListView(state)
                LeftPanelMode.TREE -> ChatTreeView(state)
            }
        }

        if (state.leftScreenCurtain.isActive) {
            val screenColor = Color(0xFFF2F2F2)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(state.leftScreenCurtain.opacity)
                    .background(screenColor)
            )
        }
    }
}
