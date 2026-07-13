package com.utilities.conduit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InputArea(state: AppState, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Row(modifier = Modifier.fillMaxWidth().height(80.dp).padding(10.dp)) {
        OutlinedTextField(value = text, singleLine = false, onValueChange = { text = it },
            placeholder = {
                val name = state.currentExpert.value?.nickname ?: "an Expert"
                Text("Type a message to $name...")
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    when {
                        keyEvent.key == Key.Escape -> { text = ""; true }
                        keyEvent.key == Key.Enter && !keyEvent.isShiftPressed -> {
                            if (text.isNotBlank() && !state.chatManager.isStreaming) { onSend(text); text = "" }
                            true
                        }
                        else -> false
                    }
                }
        )
        LaunchedEffect(state.currentExpert.value) { focusRequester.requestFocus() }

        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                    focusRequester.requestFocus()
                }
            },
            enabled = !state.chatManager.isStreaming
        ) {
            Text("Say")
        }
    }
}

//-------------------------------------------------------------------------------
