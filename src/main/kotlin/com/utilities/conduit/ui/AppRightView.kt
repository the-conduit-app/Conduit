package com.utilities.conduit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.utilities.conduit.AppState

@Composable
fun AppRightView(state: AppState, modifier: Modifier) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
//            .pointerInput(Unit) {
//                awaitEachGesture {
//                    awaitFirstDown(requireUnconsumed = false)
//                    scope.launch {
//                        state.leftScreenCurtain.show()
//                        state.leftPanelMode = LeftPanelMode.TREE
//                        state.leftScreenCurtain.hide()
//                    }
//                }
//            }
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

            // Important! If invisible, don't show the element at all or Compose won't
            // react to actions underneath, and we can't tell why easily
            // A life lesson in hiding: Just cuz you can't see it don't mean it ain't there
            if (state.notification.opacity > 0f) {
                ChatViewNotification(
                    state = state,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        val appActions = LocalActions.current
        InputArea(state) { userPrompt -> appActions.onSend(userPrompt) }
    }
}
