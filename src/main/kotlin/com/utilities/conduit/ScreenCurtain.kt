package com.utilities.conduit

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Overlay for showing/hiding what's behind (using opacity)
class ScreenCurtain (private val scope: CoroutineScope) {
    var isActive: Boolean by mutableStateOf(false)
    var opacity by mutableFloatStateOf(0f)
        private set

    // Eases opacity from 0 to 1 and back to 0 with a given delay (optionally set msg if given)
    private var job: Job? = null
    suspend fun show(duration: Int = 200) {
        isActive = true
        animate(initialValue = opacity, targetValue = 1f, animationSpec = tween(duration)) { value, _ -> opacity = value }
    }

    suspend fun hide(duration: Int = 200) {
        animate(initialValue = opacity, targetValue = 0f, animationSpec = tween(duration)) { value, _ -> opacity = value }
        isActive = false
    }
}
