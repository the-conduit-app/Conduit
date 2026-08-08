package com.utilities.conduit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppRightView(state: AppState, modifier: Modifier) {
    Column(
        modifier = modifier
    ) {
        ChatControlsView(state)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatView(state)
            }
            ChatViewNotification(
                state = state,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        val appActions = LocalActions.current
        InputArea(state,) { userPrompt -> appActions.onSend(userPrompt) }
    }
}
