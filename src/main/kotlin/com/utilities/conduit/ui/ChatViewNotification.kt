package com.utilities.conduit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilities.conduit.AppState
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import org.jetbrains.compose.resources.painterResource

// Shown as an overlay (higher z-level) at the very top of the parent ChatView (inside it)
// The visibility of this panel is controlled by state.notification.opacity which
// gets perturbed by the caller from 0->1->0 on a state.notification.trigger() call.
// Important lesson from this: See comment in AppRightView.kt
@Composable
fun ChatViewNotification(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val notification = state.notification

    if (notification.opacity <= 0f) return

    val outerShape = RoundedCornerShape(20.dp)
    val innerShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .alpha(notification.opacity)
            .clip(outerShape)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.45f),
                outerShape
            )
    ) {
        // Plasma backdrop
        Image(
            painter = painterResource(Res.drawable.plasma_s1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.75f
        )

        // Very subtle wash
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.08f))
        )

        // Inner panel
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clip(innerShape)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.5f),
                    innerShape
                )
                .background(Color.White.copy(alpha = 0.50f))
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
        ) {
            Text(
                text = notification.message,
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                color = Color(0xFF007C91)
            )
        }
    }
}
