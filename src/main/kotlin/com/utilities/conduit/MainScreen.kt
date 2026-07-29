package com.utilities.conduit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(state: AppState) {
    val appActions = LocalActions.current

    Row(modifier = Modifier.fillMaxSize()) {
        LeftPanelContainer(state = state, modifier = Modifier.weight(1f).fillMaxHeight())
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatControlsView(state)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChatView(state)
                    }
                    NotificationView(
                        state = state,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                InputArea(state, onSend = { userPrompt -> appActions.onSend(userPrompt) })
            }
        }
    }
}
