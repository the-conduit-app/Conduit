package com.utilities.conduit.maintenance

// Various Low priority maintenance jobs
// 1. runTitleMaintenance: Periodically scan all chats and rename potential candidates automatically
// 2. generate internal navigation summaries for branching nodes
// 3. Create and update user model

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.utilities.conduit.AppJson
import com.utilities.conduit.AppState
import com.utilities.conduit.UserModel
import com.utilities.conduit.chat.ChatSummary
import com.utilities.conduit.utils.ChatUtils
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.coroutines.cancellation.CancellationException
import kotlin.streams.asSequence
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

// Periodic cleanup/org routines when app is idle
object Maintenance {
    private const val IDLE_TIMEOUT = 5_000L //// todo chg
    private var maintenanceJob: Job? = null
    private var appState: AppState? = null

    suspend fun start(state: AppState) {
        delay(30_000.milliseconds) // Startup grace period
        appState = state

        //Trace.log("Maint: STARTING")
        while (currentCoroutineContext().isActive) {
            delay(IDLE_TIMEOUT.milliseconds)
            val currentAppState = appState ?: continue

            if (currentAppState.chatManager.isGenerating
                || System.currentTimeMillis() - currentAppState.lastUserActivity < IDLE_TIMEOUT) {
                continue
            }

            maintenanceJob = currentAppState.scope.launch(Dispatchers.Default) {
                try {
                    runMaintenance()
                } catch (e: CancellationException) {  // TODO - check unused
                    Trace.log("MAINT: CANCELLED")
                } finally {
                    maintenanceJob = null
                }
            }
            maintenanceJob?.join()
        }
    }

    fun onUserActivity() {
        if (maintenanceJob?.isActive == true) {
            cancel()
        }
    }

    fun cancel() {
        if (maintenanceJob?.isActive != true) return

        Trace.log("Maintenance: cancel")
        appState?.systemExpert?.abortResponse()
        maintenanceJob?.cancel()
    }

    suspend fun stop() {
        val job = maintenanceJob ?: return

        //Trace.log("MAINT: STOPPING")
        cancel()
        job.cancel()
        job.join()
        //Trace.log("MAINT: STOPPED")
    }

    private suspend fun runMaintenance() {
        delay(2000.milliseconds)
        if (appState?.systemExpert?.sessionPtr == null) {
            Trace.log("MAINT: Round skipped — system expert unavailable")
            return
        }

        Trace.log("Starting Maintenance")
        currentCoroutineContext().ensureActive()
        runTitleMaintenance()

        currentCoroutineContext().ensureActive()
        runHistorySummaryMaintenance()

        currentCoroutineContext().ensureActive()
        runChatSummaryMaintenance()

        currentCoroutineContext().ensureActive()
        runUserModelMaintenance()
    }

    // Rename a *single* anonymous chat, if found, and return
    suspend fun runTitleMaintenance() {
        val systemExpert = appState?.systemExpert
        if (systemExpert?.sessionPtr == null) {
            Trace.log("Maintenance early ret - sysexpert.session = ${systemExpert?.sessionPtr}")
            return
        }
        //Trace.log("RunTitlemaint CGE = ${state.chatManager.currentlyGeneratingExpert}")

        val chatsList = appState?.chatsList ?: return

        for (item in chatsList.items.toList()) {
            if (!item.chat.title.equals("Welcome to Conduit", ignoreCase = true))
                continue

            //Trace.log("RunTitlemaint ${item.chat.title} numnodes = ${item.chat.nodes.size}")

            if (item.chat.nodes.size < 3)
                continue

            if (appState?.chatManager?.currentlyGeneratingExpert != null)
                return

            //Trace.log("Rename generating new title")
            val newTitle = MaintenanceUtils.generateChatTitle(systemExpert, item.chat)
            //Trace.log("Rename generated new title = $newTitle")

            if (newTitle.equals(item.chat.title, ignoreCase = true))
                continue

            val updatedChat = chatsList.rename(item, newTitle, needsHumanReview = true)
            if (updatedChat == null) {
                Trace.log("Rename to '$newTitle' failed; maintenance round ending")
                return
            }
            if (updatedChat.id == appState?.chatManager?.currentChat?.id) {
                withContext(Dispatchers.Main) { appState!!.chatManager.currentChat = updatedChat }
            }

            return
        }
    }

    // Generate summaries for branching nodes (with enough ancestors - checked in the helper)
    private suspend fun runHistorySummaryMaintenance() {
        val systemExpert = appState?.systemExpert
        if (systemExpert?.sessionPtr == null) {
            Trace.log("MAINT: summary skip — system expert unavailable")
            return
        }

        val chatsList = appState?.chatsList ?: return
        for (item in chatsList.items.toList()) {
            val chat = item.chat

            for (node in chat.nodes.values) {
                if (appState?.chatManager?.currentlyGeneratingExpert != null) {
                    Trace.log("Summary Generation break due to currently generating expert")
                    return
                }

                if (node.children.size < 2)
                    continue
                if (node.historySummary != null)
                    continue

                //Trace.log("MAINT: generating history summary for node ${node.id} in chat ${chat.title}")

                val summary = MaintenanceUtils.generateHistorySummary(systemExpert, chat, node)
                if (summary.isNullOrBlank())
                    continue

                node.historySummary = summary
                ChatUtils.saveChatToDisk(chat)
                //Trace.log("MAINT: generated and saved history summary for node ${node.id}")

                return
            }
        }
    }

    // Generate summaries of chats in the chats/chat-summaries/folder
    // TODO restore private after testing
    suspend fun runChatSummaryMaintenance() {
        val systemExpert = appState?.systemExpert
        if (systemExpert?.sessionPtr == null) {
            Trace.log("MAINT: chat summary skip — system expert unavailable")
            return
        }

        val summariesDir = Paths.get(AppUtils.getChatsDir(), "chat-summaries")

        withContext(Dispatchers.IO) {
            Files.createDirectories(summariesDir)
        }

        val items = appState?.chatsList?.items ?: return
        for (item in items.toList()) {
            if (appState?.chatManager?.currentlyGeneratingExpert != null) {
                Trace.log("MAINT: chat summary break due to currently generating expert")
                return
            }

            //Trace.log("Summarizing chat ${item.chat.title}")
            val chat = item.chat
            val summaryFile = summariesDir.resolve("${chat.id}.json")

            val previousSummary: ChatSummary? = withContext(Dispatchers.IO) {
                if (!Files.exists(summaryFile)) {
                    null
                } else {
                    runCatching {
                        AppJson.decodeFromString<ChatSummary>(Files.readString(summaryFile))
                    }.getOrNull()
                }
            }

            val needsSummary = previousSummary == null || previousSummary.chatId != chat.id ||
                        previousSummary.chatModifiedTime < item.modificationTime
            if (!needsSummary) {
                //Trace.log("skipping - doesn't need summary")
                continue
            }

            //Trace.log("MAINT: generating chat summary for ${chat.title}")

            val summaryText = MaintenanceUtils.generateChatSummary(systemExpert, chat, previousSummary)
            if (summaryText.isBlank())
                continue

            val chatSummary = ChatSummary(
                chat.id,
                chat.title,
                item.modificationTime,
                summaryText
            )

            withContext(Dispatchers.IO) {
                Files.writeString(
                    summaryFile,
                    AppJson.encodeToString(chatSummary)
                )
            }

            //Trace.log("MAINT: generated and saved chat summary for ${chat.title}")
            return
        }
    }

    // Generate a model of the user from various chats/chat-summaries/*.json
    // TODO restore private after testing
     suspend fun runUserModelMaintenance() {
        val systemExpert = appState?.systemExpert

        if (systemExpert?.sessionPtr == null) {
            Trace.log("MAINT: user model skip — system expert unavailable")
            return
        }

        val summariesDir = Paths.get(AppUtils.getChatsDir(), "chat-summaries")

        if (!Files.exists(summariesDir)) {
            //Trace.log("MAINT: user model skip — no chat summaries directory")
            return
        }

        // We only pick up ONE chat summary file modified LATER than the last summary file already processed
        val previousConduitUserModel = appState?.conduitUserModel
        val lastSummaryModifiedTime = previousConduitUserModel?.lastSummaryModifiedTime ?: 0L

        // We need to capture both the summary itself, and its file modification time
        data class NewSummary(
            val modifiedTime: Long,
            val chatSummary: ChatSummary
        )
        val newSummary: NewSummary? = withContext(Dispatchers.IO) {
            Files.list(summariesDir).use { stream ->
                stream.asSequence()
                    .filter { it.fileName.toString().endsWith(".json") }
                    .mapNotNull { path ->
                        try {
                            val modifiedTime = Files.getLastModifiedTime(path).toMillis()
                            if (modifiedTime <= lastSummaryModifiedTime) {
                                return@mapNotNull null
                            }
                            NewSummary(modifiedTime, AppJson.decodeFromString<ChatSummary>(Files.readString(path)))
                        } catch (e: Exception) {
                            Trace.log("MAINT: user model — skipping mangled " + "${path.fileName}: ${e.message}")
                            null
                        }
                    }
                    .minByOrNull { it.modifiedTime }
            }
        }
        if (newSummary == null) {
            //Trace.log("User Model gen: No new summary found - returning")
            return
        }

        if (appState?.chatManager?.currentlyGeneratingExpert != null) {
            Trace.log("User Model generation break due to currently generating expert")
            return
        }

        val userModelStr = MaintenanceUtils.generateUserModel(systemExpert, newSummary.chatSummary)
        if (userModelStr.isBlank()) {
            Trace.log("MAINT: user model generation returned blank")
            return
        }

        val updatedConduitUserModel = UserModel.updateConduitUserModel(
            userModelStr,
            newSummary.modifiedTime
        )

        if (appState != null) {
            appState!!.conduitUserModel = updatedConduitUserModel
        }

        //Trace.log("MAINT: generated user model from ${newSummary.chatSummary.chatId}")
    }
}
