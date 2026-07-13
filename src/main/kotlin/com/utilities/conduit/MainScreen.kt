package com.utilities.conduit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(state: AppState) {
    val appActions = LocalActions.current

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            TreeView(state)
        }

        Column(modifier = Modifier.weight(2f).fillMaxHeight()) {
            ChatControlsView(state)
            ChatView(state)
            InputArea(state, onSend = { userPrompt -> appActions.onSend(userPrompt) })
        }
    }
}
