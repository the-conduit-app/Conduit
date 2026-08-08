package com.utilities.conduit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class LeftPanelMode {
    LIST,
    TREE
}

@Composable
fun AppLeftView(state: AppState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {

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
                // New chat (List mode only) ----------------------------------
                if (state.leftPanelMode == LeftPanelMode.LIST) {
                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        modifier = Modifier.size(24.dp),
                        enabled = state.chatManager.currentlyGeneratingExpert == null,
                        onClick = {
                            val chat = state.chatManager.createChat()
                            Trace.log("Created new chat ${chat.id}")
                            state.focusInput.value++
                            state.chatManager.currentChat = chat
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New chat")  // "+" button
                    }
                }

                // Toggle -----------------------------------------------------
                Text(
                    text = if (state.leftPanelMode == LeftPanelMode.LIST) "🌲" else "☰",
                    modifier = Modifier.clickable {
                        state.leftPanelMode =
                            if (state.leftPanelMode == LeftPanelMode.LIST)
                                LeftPanelMode.TREE
                            else
                                LeftPanelMode.LIST
                    }
                )
            }
        }

        HorizontalDivider()

        // Body ---------------------------------------------------------------
        when (state.leftPanelMode) {
            LeftPanelMode.LIST -> ChatsListView(state)
            LeftPanelMode.TREE -> ChatTreeView(state)
        }
    }
}
