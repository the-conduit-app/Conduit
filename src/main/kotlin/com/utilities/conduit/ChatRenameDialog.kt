package com.utilities.conduit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch

@Composable
fun ChatRenameDialog(
    initialTitle: String,
    onCancel: () -> Unit,
    onSuggest: suspend () -> String,
    onOk: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSuggesting by remember { mutableStateOf(false) }

    var chatTitle by remember(initialTitle) {
        mutableStateOf(initialTitle)
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onCancel
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Rename Chat",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    enabled = !isSuggesting,
                    value = chatTitle,
                    onValueChange = { chatTitle = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Enter -> {
                                    val title = chatTitle.trim()
                                    if (title.isNotEmpty()) {
                                        onOk(title)
                                        true
                                    } else
                                        false
                                }
                                Key.Escape -> {
                                    onCancel()
                                    true
                                }
                                else -> false
                            }
                        }
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        enabled = !isSuggesting,
                        onClick = {
                            scope.launch {
                                isSuggesting = true
                                try {
                                    chatTitle = onSuggest()
                                } finally {
                                    isSuggesting = false
                                }
                            }
                        }
                    ) {
                        Text(if (isSuggesting) "Reviewing..." else "Suggest a Title")
                    }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        enabled = !isSuggesting,
                        onClick = {
                            val title = chatTitle.trim()
                            if (title.isNotEmpty()) {
                                onOk(title)
                            }
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
