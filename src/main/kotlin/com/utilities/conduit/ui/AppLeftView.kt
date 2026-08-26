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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import kotlinx.coroutines.launch

val LocalLeftViewOpacity = compositionLocalOf<ViewOpacity> {
    error("LocalLeftViewOpacity not provided")
}

enum class LeftPanelMode {
    LIST,
    TREE
}

@Composable
fun AppLeftView(state: AppState, modifier: Modifier = Modifier) {
    val opacity = LocalLeftViewOpacity.current
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.alpha(opacity.value)) {
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
                            .let { if (it.length > 30) it.take(27) + "..." else it }
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
                                        //opacity.hide()
                                        state.leftPanelMode = LeftPanelMode.LIST
                                        //opacity.show()
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
    }
}
