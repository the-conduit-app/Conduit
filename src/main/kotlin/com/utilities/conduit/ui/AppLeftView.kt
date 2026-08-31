package com.utilities.conduit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

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
    var settingsMenuExpanded by remember { mutableStateOf(false) }

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
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "New chat",
                                    tint = Color(0xFF007C91)
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
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Chat list",
                                    tint = Color(0xFF007C91)
                                )
                            }
                        }
                    }
                    // Sound on/off ---------------------------------------------------------

                    IconButton(
                        modifier = Modifier.size(18.dp),
                        onClick = {
                            Sounds.isSilent = !Sounds.isSilent
                        }
                    ) {
                        Icon(
                            imageVector = if (Sounds.isSilent)
                                Icons.AutoMirrored.Filled.VolumeOff
                            else
                                Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (Sounds.isSilent) "Sounds on" else "Sounds off",
                            tint = Color(0xFF007C91)
                        )
                    }
                }
            }

            // Horizongal divider
            Image(
                painter = painterResource(Res.drawable.plasma_s1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            )

            // Body ---------------------------------------------------------------
            when (state.leftPanelMode) {
                LeftPanelMode.LIST -> ChatsListView(state)
                LeftPanelMode.TREE -> ChatTreeView(state)
            }
        }
    }
}
