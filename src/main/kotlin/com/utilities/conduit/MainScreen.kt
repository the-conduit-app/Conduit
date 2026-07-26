package com.utilities.conduit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(state: AppState) {
    val appActions = LocalActions.current

    Row(modifier = Modifier.fillMaxSize()) {
        LeftPanelContainer(state = state, modifier = Modifier.weight(1f).fillMaxHeight())
        Column(modifier = Modifier.weight(2f).fillMaxHeight()) {
            ChatControlsView(state)
            ChatView(state)
            InputArea(state, onSend = { userPrompt -> appActions.onSend(userPrompt) })
        }
    }
}
