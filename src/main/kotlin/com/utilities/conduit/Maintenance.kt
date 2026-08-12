// Various Low priority maint jobs
// 1. runTitleMaintenance: Periodically scan all chats and rename potential candidates automatically
//
package com.utilities.conduit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
object Maintenance {
    private var maintenanceJob: Job? = null
    private var state: AppState? = null

    fun start(appState: AppState, scope: CoroutineScope) {
        state = appState
        val chatsList = appState.chatsList

        if (appState.chatManager.currentlyGeneratingExpert != null)
            return

        maintenanceJob?.cancel()
        maintenanceJob = scope.launch(Dispatchers.Default) {
            try {
                runTitleMaintJob(appState, chatsList)
            } catch (e: CancellationException) {
                Trace.log("Current maintenance round cancelled")
            } finally {
                maintenanceJob = null
            }
        }
    }

    fun cancel() {
        state?.systemExpert?.abortResponse()
        maintenanceJob?.cancel()
        maintenanceJob = null
    }

    fun stop() {
        maintenanceJob?.cancel()
        maintenanceJob = null
    }

    // Rename a *single* anonymous chat, if found, and return
     suspend fun runTitleMaintJob(state: AppState, chatsList: ChatsList) {
        val systemExpert = state.systemExpert

        if (systemExpert.sessionPtr == null) {
            Trace.log("Maintenance early ret - sysexpert.session = ${systemExpert.sessionPtr}")
            return
        }

        for (item in chatsList.items.toList()) {
            if (!item.chat.title.equals("Welcome to Conduit", ignoreCase = true))
                continue

            if (item.chat.nodes.size < 3)
                continue
            if (state.chatManager.currentlyGeneratingExpert != null)
                return

            Trace.log("Rename generating new title")
            val newTitle = ChatUtils.generateChatTitle(systemExpert, item.chat)
            Trace.log("Rename generated new title = $newTitle")

            if (newTitle.equals(item.chat.title, ignoreCase = true))
                continue

            chatsList.rename(item, newTitle)
            chatsList.setNeedsHumanReview(item.chat.id, true)

            return
        }
    }
}
