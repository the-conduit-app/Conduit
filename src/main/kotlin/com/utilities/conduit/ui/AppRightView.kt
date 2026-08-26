package com.utilities.conduit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import com.utilities.conduit.AppState

val LocalRightViewOpacity = compositionLocalOf<ViewOpacity> {
    error("LocalRightViewOpacity not provided")
}

@Composable
fun AppRightView(state: AppState, modifier: Modifier) {
    val appActions = LocalActions.current
    val opacity = LocalRightViewOpacity.current

    Column(modifier = modifier) {
        ChatControlsView(state)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .alpha(opacity.value)
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

        InputArea(state) { userPrompt -> appActions.onSend(userPrompt) }
    }
}

class ViewOpacity {
    private val animatable = Animatable(1f)

    val value: Float
        get() = animatable.value

    suspend fun hide() {
        animatable.animateTo(
            0f,
            tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing
            )
        )
    }

    suspend fun show() {
        animatable.animateTo(
            1f,
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        )
    }
}
