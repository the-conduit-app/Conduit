package com.utilities.conduit

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@Composable
fun AppRightView(state: AppState, modifier: Modifier) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch {
                        state.leftScreenCurtain.show()
                        state.leftPanelMode = LeftPanelMode.TREE
                        state.leftScreenCurtain.hide()
                    }
                }
            }
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
