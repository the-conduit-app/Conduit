package com.utilities.conduit

// Various Low priority maintenance jobs
// 1. runTitleMaintenance: Periodically scan all chats and rename potential candidates automatically
// 2. generate internal navigation summaries for branching nodes
// 3. Create and update user model

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.ChatMessage
import com.utilities.conduit.chat.ChatSummary
import com.utilities.conduit.chat.ChatUtils
import com.utilities.conduit.chat.MessageAuthor
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.AppJson
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
import kotlin.io.path.writeText
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
        if (maintenanceJob?.isActive == true) {
            cancel()
        }
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

        val chatsList = state.chatsList
        for (item in chatsList.items.toList()) {
            if (state.chatManager.currentlyGeneratingExpert != null) {
                Trace.log("MAINT: chat summary break due to currently generating expert")
                return
            }

            val chat = item.chat
            val summaryFile = summariesDir.resolve("${chat.id}.json")

            val needsSummary = if (!Files.exists(summaryFile)) {
                true
            } else {
                val summary = runCatching {
                    AppJson.decodeFromString<ChatSummary>(
                        Files.readString(summaryFile)
                    )
                }.getOrNull()

                summary == null ||
                        summary.sourceModifiedTime < item.modificationTime ||
                        summary.chatId != chat.id
            }

            if (!needsSummary)
                continue

            Trace.log("MAINT: generating chat summary for ${chat.title}")

            val summaryText = ChatUtils.generateChatSummary(
                systemExpert,
                chat
            )

            if (summaryText.isBlank())
                continue

            val chatSummary = ChatSummary(
                chatId = chat.id,
                chatTitle = chat.title,
                sourceModifiedTime = item.modificationTime,
                summary = summaryText
            )

            withContext(Dispatchers.IO) {
                Files.writeString(summaryFile, AppJson.encodeToString(chatSummary))
            }

            Trace.log("MAINT: generated chat summary for ${chat.title}")

            return
        }
    }
}

private suspend fun runUserModelMaintenance(state: AppState) {
    currentCoroutineContext().ensureActive()

    val systemExpert = state.systemExpert

    if (systemExpert.sessionPtr == null) {
        Trace.log("MAINT: user model skip — system expert unavailable")
        return
    }

    val chatsDir = Paths.get(AppUtils.getChatsDir())
    val appDir = Paths.get(AppUtils.getAppDir())
    val summariesDir = chatsDir.resolve("chat-summaries")
    val userModelFile = appDir.resolve("user-model.json")

    if (!Files.exists(summariesDir)) {
        Trace.log("MAINT: user model skip — no chat summaries directory")
        return
    }

    val userModelModifiedTime = withContext(Dispatchers.IO) {
        if (Files.exists(userModelFile)) {
            Files.getLastModifiedTime(userModelFile).toMillis()
        } else {
            0L
        }
    }

    val newSummary: ChatSummary? = withContext(Dispatchers.IO) {
        Files.list(summariesDir).use { stream ->
            stream
                .filter { it.fileName.toString().endsWith(".json") }
                .toList()
                .mapNotNull { path ->
                    try {
                        AppJson.decodeFromString<ChatSummary>(
                            Files.readString(path)
                        )
                    } catch (e: Exception) {
                        Trace.log(
                            "MAINT: user model — unable to read ${path.fileName}: ${e.message}"
                        )
                        null
                    }
                }
                .filter {
                    it.sourceModifiedTime > userModelModifiedTime
                }
                .minByOrNull { it.sourceModifiedTime }
        }
    }

    if (newSummary == null) {
        //Trace.log("MAINT: user model — nothing new")
        return
    }

    val existingUserModel = withContext(Dispatchers.IO) {
        if (Files.exists(userModelFile)) {
            Files.readString(userModelFile)
        } else {
            "NO EXISTING USER MODEL"
        }
    }

    val newInformation = """
        Conversation: ${newSummary.chatTitle}

        ${newSummary.summary}
    """.trimIndent()

    val promptText = PROMPTS.USER_MODEL_GENERATION
        .replace("{EXISTING_USER_MODEL}", existingUserModel)
        .replace("{NEW_INFORMATION}", newInformation)

    val messages = mutableListOf(
        ChatMessage(
            author = MessageAuthor(type = AuthorType.USER),
            text = promptText
        )
    )

    val prompt = AppUtils.buildPrompt(systemExpert, messages)

    Trace.log(
        "USER MODEL GEN START session=${systemExpert.sessionPtr} " +
                "summary=${newSummary.chatId}"
    )

    val result = StringBuilder()

    LlmPortal.getResponse(systemExpert.sessionPtr!!, prompt)
        .collect { token ->
            currentCoroutineContext().ensureActive()
            result.append(token)
        }

    val newUserModel = result.toString().trim()

    Trace.log("USER MODEL GEN END session=${systemExpert.sessionPtr}")

    if (newUserModel.isBlank()) {
        Trace.log("MAINT: user model generation returned blank")
        return
    }

    withContext(Dispatchers.IO) {
        userModelFile.writeText(newUserModel)
    }

    Trace.log(
        "MAINT: generated user model from ${newSummary.chatId}"
    )
}
