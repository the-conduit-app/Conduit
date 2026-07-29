package com.utilities.conduit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// The NotificationView is simply a permanent overlay at the top center of the ChatView
// RHS Panel. Transluncent. It should have a companion method called xyz(string) which will
// animate it in and then out again after setting the contents.
@Composable
fun NotificationView(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val message = state.notification.value

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(1500))
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 24.dp)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF3CD).copy(alpha = 0.92f),
            shadowElevation = 8.dp
        ) {
            Text(
                text = message ?: "",
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5C4500)
            )
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3000.milliseconds)
            state.notification.value = null
        }
    }
}
