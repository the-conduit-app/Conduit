package com.utilities.conduit

// Various Low priority maintenance jobs
// 1. runTitleMaintenance: Periodically scan all chats and rename potential candidates automatically
// 2. generate internal navigation summaries for branching nodes
// 3. Create and update user model

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.utilities.conduit.chat.ChatUtils
import com.utilities.conduit.debug.Trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

// Important note about Maint jobs (see ChatManager also):
// When generating with SystemExpert, these jobs do NOT call
// ChatManager.onBeginResponse() etc. Instead, they cancel their running
// sessions and yield whenever another generation is requested.

// Global modifier to detect (and pass thru user activity)
fun Modifier.userActivityMonitor(state: AppState): Modifier =
    this.onPreviewKeyEvent {
        state.lastUserActivity = System.currentTimeMillis()
        Maintenance.onUserActivity()
        false
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()

                state.lastUserActivity = System.currentTimeMillis()
                Maintenance.onUserActivity()
            }
        }
    }

object Maintenance {
    private const val IDLE_TIMEOUT = 5_000L
    private var maintenanceJob: Job? = null
    private var state: AppState? = null

    suspend fun start(state: AppState, scope: CoroutineScope) {
        Maintenance.state = state
        //Trace.log("Maint: STARTING")
        while (currentCoroutineContext().isActive) {
            delay(IDLE_TIMEOUT.milliseconds)
            if (state.chatManager.isGenerating
                || System.currentTimeMillis() - state.lastUserActivity < IDLE_TIMEOUT) {
                continue
            }

            maintenanceJob = scope.launch(Dispatchers.Default) {
                try {
                    runMaintenance(state)
                } catch (e: CancellationException) {
                    Trace.log("MAINT: CANCELLED")
                } finally {
                    maintenanceJob = null
                }
            }
            maintenanceJob?.join()
        }
    }

    fun onUserActivity() {
        maintenanceJob?.cancel()
    }

    fun cancel() {
        Trace.log("Maintenance: cancel")
        state?.systemExpert?.abortResponse()
        maintenanceJob?.cancel()
    }

    suspend fun stop() {
        val job = maintenanceJob ?: return

        //Trace.log("MAINT: STOPPING")
        Maintenance.cancel()
        job.cancel()
        job.join()
        //Trace.log("MAINT: STOPPED")
    }

    private suspend fun runMaintenance(state: AppState) {
        //Trace.log("RunMaint: STARTING")
        currentCoroutineContext().ensureActive()
        runTitleMaintenance(state)
        currentCoroutineContext().ensureActive()
        runHistorySummaryMaintenance(state)
        //Trace.log("RunMaint: Finished")
    }

    // Rename a *single* anonymous chat, if found, and return
    suspend fun runTitleMaintenance(state: AppState) {
        val systemExpert = state.systemExpert

        if (systemExpert.sessionPtr == null) {
            Trace.log("Maintenance early ret - sysexpert.session = ${systemExpert.sessionPtr}")
            return
        }
        //Trace.log("RunTitlemaint CGE = ${state.chatManager.currentlyGeneratingExpert}")

        val chatsList = state.chatsList
        for (item in chatsList.items.toList()) {
            if (!item.chat.title.equals("Welcome to Conduit", ignoreCase = true))
                continue

            //Trace.log("RunTitlemaint ${item.chat.title} numnodes = ${item.chat.nodes.size}")

            if (item.chat.nodes.size < 3)
                continue

            if (state.chatManager.currentlyGeneratingExpert != null)
                return

            //Trace.log("Rename generating new title")
            val newTitle = ChatUtils.generateChatTitle(systemExpert, item.chat)
            //Trace.log("Rename generated new title = $newTitle")

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

    private suspend fun runHistorySummaryMaintenance(state: AppState) {
        val systemExpert = state.systemExpert

        if (systemExpert.sessionPtr == null) {
            Trace.log("MAINT: summary skip — system expert unavailable")
            return
        }

        val chatsList = state.chatsList
        for (item in chatsList.items.toList()) {
            val chat = item.chat

            for (node in chat.nodes.values) {
                if (state.chatManager.currentlyGeneratingExpert != null) {
                    Trace.log("Summary Generation break due to currently generating expert")
                    return
                }

                if (node.children.size < 2)
                    continue
                if (node.historySummary != null)
                    continue

                //Trace.log("MAINT: generating history summary for node ${node.id} in chat ${chat.title}")

                val summary = ChatUtils.generateHistorySummary(systemExpert, chat, node)
                if (summary.isBlank())
                    continue

                node.historySummary = summary
                ChatUtils.saveChatToDisk(chat)

                //Trace.log("MAINT: generated history summary for node ${node.id}")

                return
            }
        }
    }
}
