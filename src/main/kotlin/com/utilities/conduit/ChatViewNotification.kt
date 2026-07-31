package com.utilities.conduit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Shown as an overlay (higher z-level) at the very top of the parent ChatView (inside it)
// The visibility of this panel is controlled by state.notification.opacity which
// gets perturbed by the caller from 0->1->0 on a state.notification.trigger() call.
@Composable
fun ChatViewNotification(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val notification = state.notification

    Surface(
        modifier = modifier
            .alpha(notification.opacity)
            .padding(top = 24.dp)
            .widthIn(max = 400.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF3CD),
        shadowElevation = 8.dp
    ) {
        Text(
            text = notification.message,
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
