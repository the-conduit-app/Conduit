package com.utilities.conduit.ui.chatsList

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.LocalTextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.ui.ConduitProgressIndicator
import com.utilities.conduit.ui.ConduitTheme
import com.utilities.conduit.ui.Sounds
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import conduit.generated.resources.plasma_s64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun ChatRenameDialog(
    initialTitle: String,
    onCancel: () -> Unit,
    onSuggest: suspend () -> String,
    onOk: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSuggesting by remember { mutableStateOf(false) }
    var suggestJob by remember { mutableStateOf<Job?>(null) } // So Cancel can cancel

    var chatTitle by remember(initialTitle) {
        mutableStateOf(initialTitle)
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = {
            if (!isSuggesting) { // Ignore outside clicks while busy.
                onCancel()
            }
        }
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.width(420.dp)
        ) {
            Box {
                Image(
                    painter = painterResource(Res.drawable.plasma_s1),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // title
                    Text(
                        text = "Rename Chat",
                        color = Color.Black.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(20.dp))

                    // text input box under the title
                    OutlinedTextField(
                        enabled = !isSuggesting,
                        value = chatTitle,
                        onValueChange = { chatTitle = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = Color.Black
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007C91).copy(alpha = 0.4f),
                            unfocusedBorderColor = Color(0xFF007C91).copy(alpha = 0.2f),
                            focusedContainerColor = Color.White.copy(alpha = 0.25f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        trailingIcon = {
                            if (isSuggesting) {
                                ConduitProgressIndicator(
                                    images = listOf(
                                        painterResource(Res.drawable.plasma_s64),
                                        painterResource(Res.drawable.plasma_s1)
                                    ),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        },
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

                    // bot row: cancel, suggest, ok
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = {
                                if (suggestJob?.isActive == true) {
                                    Sounds.Braking.play()
                                    Trace.log("Cancelling generation job $suggestJob")
                                    suggestJob?.cancel()
                                    suggestJob = null
                                }
                                onCancel()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.Black.copy(alpha = 0.75f)
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                style = ConduitTheme.Typography.labelLarge
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // Suggest a title button
                        OutlinedButton(
                            enabled = !isSuggesting,
                            onClick = {
                                Sounds.Whoosh.play()
                                isSuggesting = true

                                suggestJob = scope.launch(Dispatchers.IO) {
                                    try {
                                        val title = onSuggest() // is MaintenanceUtils.generateChatTitle
                                        Sounds.Ting.play()
                                        withContext(Dispatchers.Main) {
                                            chatTitle = title
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            isSuggesting = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.Black.copy(alpha = 0.75f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.65f)
                            )
                        ) {
                            Text(if (isSuggesting) "Reviewing Chat" else "Suggest a Title")
                        }
                        Spacer(Modifier.width(12.dp))

                        // OK button
                        Button(
                            enabled = !isSuggesting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.Black.copy(alpha = 0.75f)
                            ),
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
}

/*
// ...



// ...
 */
