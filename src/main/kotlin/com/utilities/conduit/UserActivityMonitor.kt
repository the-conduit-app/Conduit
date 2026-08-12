package com.utilities.conduit

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

fun Modifier.userActivityMonitor(): Modifier =
    this.onPreviewKeyEvent {
        UserActivityMonitor.onUserActivity()
        false
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()
                UserActivityMonitor.onUserActivity()
            }
        }
    }


object UserActivityMonitor {
    private const val IDLE_TIMEOUT_MS = 10_000L

    private var scope: CoroutineScope? = null
    private var idleJob: Job? = null
    private var idleAction: (suspend () -> Unit)? = null

    fun start(
        appScope: CoroutineScope,
        onIdleJob: suspend () -> Unit
    ) {
        scope = appScope
        idleAction = onIdleJob
        restartTimer()
    }

    fun onUserActivity() {
        restartTimer()
    }

    private fun restartTimer() {
        val appScope = scope ?: return
        val job = idleAction ?: return

        idleJob?.cancel()

        idleJob = appScope.launch {
            delay(IDLE_TIMEOUT_MS.milliseconds)
            job()
        }
    }

    fun stop() {
        idleJob?.cancel()
        idleJob = null
        idleAction = null
        scope = null
    }
}
