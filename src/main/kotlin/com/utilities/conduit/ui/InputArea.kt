package com.utilities.conduit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState

@Composable
fun InputArea(state: AppState, onSend: (String) -> Unit) {
    @Suppress("UNUSED_VARIABLE")
    val version = state.chatManager.version // Observe structural chat changes

    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isGenerating = state.chatManager.isGenerating

    // value incremented by ChatsListView when selecting a new chat (to force recomp)
    // User is free to switch experts via the Experts panel (for next prompt) during generation
    LaunchedEffect(state.focusInput.value, state.currentExpert.value, isGenerating) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF007F86).copy(alpha = 0.45f),
                focusedBorderColor = Color(0xFF007F86).copy(alpha = 0.60f)
            ),
            value = text,
            onValueChange = { text = it },
            singleLine = false,
            minLines = 1,
            maxLines = 6,
            enabled = !isGenerating,
            placeholder = {
                val name = state.currentExpert.value?.nickname ?: "an Expert"
                androidx.compose.material3.Text("Type a message to $name...")
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 60.dp, max = 180.dp)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { ev ->
                    when {
                        ev.key == Key.Escape -> {
                            text = ""
                            Sounds.Hsoohw.play()
                            println("ESC")
                            true
                        }

                        ev.isShiftPressed -> false

                        ev.key == Key.Enter -> {
                            if (text.isNotBlank() && !isGenerating) {
                                Sounds.Whoosh.play()
                                onSend(text)
                                text = ""
                            }
                            true
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
                    Sounds.Whoosh.play()
                    onSend(text)
                    text = ""
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGenerating) {
                    Color(0xFFD61F2C)
                } else {
                    Color(0xFF007C91)
                },
                disabledContainerColor = Color(0xFF007C91).copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.4f),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (isGenerating) {
                    Icons.Filled.Stop
                } else {
                    Icons.Filled.ArrowUpward
                },
                contentDescription = if (isGenerating) "Stop" else "Send"
            )
        }
    }
}
