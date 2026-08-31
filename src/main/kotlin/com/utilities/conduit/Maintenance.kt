package com.utilities.conduit

// Various Low priority maintenance jobs
// 1. runTitleMaintenance: Periodically scan all chats and rename potential candidates automatically
// 2. generate internal navigation summaries for branching nodes
// 3. Create and update user model

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.utilities.conduit.chat.ChatSummary
import com.utilities.conduit.utils.ChatUtils
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.ui.AppJson
import com.utilities.conduit.utils.AppUtils
import com.utilities.conduit.utils.MaintenanceUtils
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyles
import kotlinx.coroutines.CoroutineScope
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
        currentCoroutineContext().ensureActive()
        runTitleMaintenance(state)

        currentCoroutineContext().ensureActive()
        runHistorySummaryMaintenance(state)

        currentCoroutineContext().ensureActive()
        runChatSummaryMaintenance(state)

        currentCoroutineContext().ensureActive()
        runUserModelMaintenance(state)
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
            val newTitle = MaintenanceUtils.generateChatTitle(systemExpert, item.chat)
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

    // Generate summaries for branching nodes (with enough ancestors - checked in the helper)
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
    private suspend fun runChatSummaryMaintenance(state: AppState) {
        val systemExpert = state.systemExpert
        if (systemExpert.sessionPtr == null) {
            Trace.log("MAINT: chat summary skip — system expert unavailable")
            return
        }

        val summariesDir = Paths.get(AppUtils.getChatsDir(), "chat-summaries")

        withContext(Dispatchers.IO) {
            Files.createDirectories(summariesDir)
        }

        for (item in state.chatsList.items.toList()) {
            if (state.chatManager.currentlyGeneratingExpert != null) {
                Trace.log("MAINT: chat summary break due to currently generating expert")
                return
            }

            val chat = item.chat
            val summaryFile = summariesDir.resolve("${chat.id}.json")

            val previousSummary: ChatSummary? = withContext(Dispatchers.IO) {
                if (!Files.exists(summaryFile)) {
                    null
                } else {
                    runCatching {
                        AppJson.decodeFromString<ChatSummary>(
                            Files.readString(summaryFile)
                        )
                    }.getOrNull()
                }
            }

            val needsSummary = previousSummary == null || previousSummary.chatId != chat.id ||
                        previousSummary.chatModifiedTime < item.modificationTime
            if (!needsSummary)
                continue

            Trace.log("MAINT: generating chat summary for ${chat.title}")

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

            Trace.log("MAINT: generated and saved chat summary for ${chat.title}")
            return
        }
    }

    // Generate a model of the user from various chats/chat-summaries/*.json
    private suspend fun runUserModelMaintenance(state: AppState) {
        val systemExpert = state.systemExpert

        if (systemExpert.sessionPtr == null) {
            Trace.log("MAINT: user model skip — system expert unavailable")
            return
        }

        val summariesDir = Paths.get(AppUtils.getChatsDir(), "chat-summaries")
        val userModelFile = Paths.get(AppUtils.getAppDir(), "user-model.json")

        if (!Files.exists(summariesDir)) {
            Trace.log("MAINT: user model skip — no chat summaries directory")
            return
        }

        // We only pick up ONE chat summary file modified LATER than the last summary file already processed
        val previousUserModel = AppUtils.getUserModelFromFile()
        val lastSummaryModifiedTime = previousUserModel?.lastSummaryModifiedTime ?: 0L

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
        if (newSummary == null) return

        // A new chat summary updated after the previously most recent summary used for
        // generating the user model is now available

        val previousUserModelText = previousUserModel?.text ?: "NO PREVIOUS USER MODEL EXISTS"
        val userModelStr = MaintenanceUtils.generateUserModel(
            systemExpert, previousUserModelText, newSummary.chatSummary
        )
        if (userModelStr.isBlank()) {
            Trace.log("MAINT: user model generation returned blank")
            return
        }

        val updatedUserModel = UserModel(text = userModelStr, lastSummaryModifiedTime = newSummary.modifiedTime)
        withContext(Dispatchers.IO) {
            Files.writeString(userModelFile, AppJson.encodeToString(updatedUserModel))
        }
        state.userModel = updatedUserModel

        Trace.log("MAINT: generated user model from ${newSummary.chatSummary.chatId}")
    }
}
