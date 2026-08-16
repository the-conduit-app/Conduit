// Various Low priority maint jobs
// 1. runTitleMaintenance: Periodically scan all chats and rename potential candidates automatically
//
package com.utilities.conduit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

object Maintenance {
    private var maintenanceJob: Job? = null
    private var state: AppState? = null

    fun start(appState: AppState, scope: CoroutineScope) {
        state = appState
        val chatsList = appState.chatsList

        if (appState.chatManager.isGenerating) {
            Trace.log("MAINT: SKIP — generation active")
            return
        }

        //Trace.log("MAINT: START")
        maintenanceJob?.cancel()
        maintenanceJob = scope.launch(Dispatchers.Default) {
            try {
                runTitleMaintenanceJob(appState, chatsList)
                //Trace.log("MAINT: ROUND COMPLETE")
            } catch (e: CancellationException) {
                //Trace.log("MAINT: CANCELLED - Current maintenance round cancelled")
            } finally {
                maintenanceJob = null
            }
        }
    }

    fun cancel() {
        val job = maintenanceJob ?: return

        Trace.log("MAINT: CANCEL requested")
        state?.systemExpert?.abortResponse()
        job.cancel()
        maintenanceJob = null
    }

    fun stop() {
        maintenanceJob?.cancel()
        maintenanceJob = null
    }

    // Rename a *single* anonymous chat, if found, and return
     suspend fun runTitleMaintenanceJob(state: AppState, chatsList: ChatsList) {
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

            val updatedChat = chatsList.rename(item, newTitle, needsHumanReview = true)
            if (updatedChat == null) {
                Trace.log("Rename to '$newTitle' failed; maintenance round ending")
                return
            }
            if (updatedChat.id == state.chatManager.currentChat.id) {
                withContext(Dispatchers.Main) { state.chatManager.currentChat = updatedChat }
            }

            return
        }
    }
}
