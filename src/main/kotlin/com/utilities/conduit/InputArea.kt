package com.utilities.conduit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp

@Composable
fun InputArea(state: AppState, onSend: (String) -> Unit) {
    @Suppress("UNUSED_VARIABLE")
    val version = state.chatManager.version // Observe structural chat changes

    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isGenerating = state.chatManager.currentChat.currentLeafNode?.message?.textInProgress?.value != null

    // value incremented by ChatsListView when selecting a new chat (to force recomp)
    LaunchedEffect(state.focusInput.value, isGenerating) {
        focusRequester.requestFocus()
    }
    // User is free to switch experts via the Experts panel (for next prompt) during generation
    LaunchedEffect(state.currentExpert.value) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = false,
            minLines = 1,
            maxLines = 6,
            enabled = !isGenerating,
            placeholder = {
                val name = state.currentExpert.value?.nickname ?: "an Expert"
                Text("Type a message to $name...")
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 60.dp, max = 180.dp)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { ev ->
                    when {
                        ev.key == Key.Escape -> {
                            text = ""
                            true
                        }

                        ev.key == Key.Enter && !ev.isShiftPressed -> {
                            if (text.isNotBlank() && !isGenerating) {
                                onSend(text)
                                text = ""
                            }
                            true
                        }
                        ev.key == Key.Enter && ev.isShiftPressed -> {
                            println("SHIFT ENTER: ${ev.type}")
                            false
                        }
                        else -> false
                    }
                }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            enabled = isGenerating || text.isNotBlank(),
            onClick = {
                if (isGenerating) {
                    state.chatManager.abortCurrentResponse()
                } else if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            }
        ) {
            if (isGenerating) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            } else {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Send")
            }
        }
    }
}
