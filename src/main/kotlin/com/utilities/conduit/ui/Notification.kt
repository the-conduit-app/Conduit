package com.utilities.conduit.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// A Notification is a tiny observable model. It owns a message and an opacity.
// Calling trigger() updates the message and temporarily perturbs the opacity from 0 → 1 → 0.
// Companion composables (ChatViewNotification, TreeViewNotification, etc.)
// simply render the current state of the Notification.
// The Notification composable can be found within AppRightView.kt

class Notification (private val scope: CoroutineScope) {
    // Text currently displayed.
    var message by mutableStateOf("")
        private set
    var opacity by mutableFloatStateOf(0f)
        private set

    // Eases opacity from 0 to 1 and back to 0 with a given delay (optionally set msg if given)
    private var job: Job? = null
    fun trigger(
        msg: String,
        duration: Long = 1000L
    ) {
        job?.cancel()
        job = scope.launch {
            message = msg

            animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(500)) { value, _ -> opacity = value }
            delay(duration.milliseconds)
            animate(initialValue = 1f, targetValue = 0f, animationSpec = tween(500)) { value, _ -> opacity = value }
        }
    }
}
