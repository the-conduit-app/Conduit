package com.utilities.conduit.utils

import com.utilities.conduit.Expert
import com.utilities.conduit.PROMPTS
import com.utilities.conduit.chat.Chat
import com.utilities.conduit.chat.ChatSummary
import com.utilities.conduit.chat.Node
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.ui.AppJson
import jdk.internal.org.jline.reader.LineReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.coroutines.cancellation.CancellationException

object MaintenanceUtils {
    // ----------------------------------------------------------------------------
    // Various automatic generation routines and helpers

    // Infer a chat title from its contents (Using SystemExpert). This is invoked by both the
    // maint routine for automatic renaming of default chat names, and by the dialog for rename
    // which features a generate button
    suspend fun generateChatTitle(systemExpert: Expert, chat: Chat): String {
        ChatUtils.chatUtilsMutex.withLock {
            val oldTitle = chat.title

            val effectiveHistory: ChatUtils.EffectiveHistory =
                ChatUtils.getEffectiveNodeHistory(chat, chat.cursorNodeId, excludeSystemNodes = false)

            // NOTE: boundaryContext is the first effectiveHistory node.historySummary
            // cuz we stop there
            val chatThusFar: String = AppUtils.getChatContextAsString(
                boundaryContext = effectiveHistory.boundaryContext,
                messages = effectiveHistory.nodes.mapNotNull { it.message },
                maxAssistantTextLen = 250,
                maxUserTextLen = 1500
            )
            val titlePrompt = PROMPTS.TITLE_GENERATION.replace("{CURRENT_TITLE}", oldTitle)

            Trace.log("TITLE GEN START for $oldTitle")

            val result = StringBuilder()
            try {
                systemExpert.getResponse(chatThusFar, titlePrompt, includeUserModel = false).collect { token ->
                    result.append(token)
                }
            } catch (e: CancellationException) {
                Trace.log("TITLE GEN ABORTED for $oldTitle")
                throw e
            }
            val newTitle = sanitizeChatTitle(result.toString())

            Trace.log("TITLE GEN END ('$oldTitle', '$newTitle')")
            return newTitle.ifEmpty { oldTitle }
        }
    }
    private fun sanitizeChatTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("<\\|im_(start|end)\\|?>\\s*$"), "")
            .trim()
    }

    // Infer a summary of all nodes up to a node's parent (excluding the node itself). This is
    // then stored and persisted within the node object in the chat by a maint routine.
    // History summaries are used to compress past context succinctly when we trace upwards
    // from the cursor to build context for a prompt.
    // NOTE: Skip if node has fewer than HISTORY_LENGTH_THRESHOLD predecessors (Aug 31)
    private val HISTORY_LENGTH_THRESHOLD = 20
    suspend fun generateHistorySummary(systemExpert: Expert, chat: Chat, node: Node): String? {
        ChatUtils.chatUtilsMutex.withLock {

            val effectiveHistory = ChatUtils.getEffectiveNodeHistory(chat, node.parentId, excludeSystemNodes = true)
            if (effectiveHistory.nodes.size < HISTORY_LENGTH_THRESHOLD) return null

            // The history summary for `node` describes everything leading up to and including
            // the current node's parent (excludes current node)
            // Note: boundaryContext is the context before the first history node
            val chatThusFar = AppUtils.getChatContextAsString(
                boundaryContext = effectiveHistory.boundaryContext,
                messages = effectiveHistory.nodes.mapNotNull { it.message },
                maxAssistantTextLen = 250,
                maxUserTextLen = 1500
            )

            Trace.log("MAINT: HISTORY SUMMARY START node=${node.id}")
            val summaryPrompt = PROMPTS.HISTORY_SUMMARY_GENERATION
            val result = StringBuilder()
            try {
                systemExpert.getResponse(chatThusFar, summaryPrompt, includeUserModel = false).collect { token ->
                    result.append(token)
                }
            } catch (e: CancellationException) {
                Trace.log("MAINT: HISTORY SUMMARY ABORTED node=${node.id}")
                throw e
            }
            val resultStr = result.toString().trim()
            Trace.log("MAINT: HISTORY SUMMARY END node=${node.id}, result = $resultStr")

            return resultStr
        }
    }

    // Generate a summary of the single named chat. Why? The maint routine periodically summarizes
    // chats in the background (to chats/chat-summaries). These summaries are picked up by
    // another background maint job (see next) that updates the current user model
    // APPDIR/user-model.json (expert.seedPrompt and user-model ride on every user prompt)
    // Another important note: At each update we summarize the current path from root to cursor.
    // When the cursor changes, the result is the AUGMENTED old+new summary.
    // Because the chat is a branching object, the cursor could change between invocations
    // of this function. Thus, the likelihood of a user visiting a particular branch in
    // a conversation is related to that of the branch being reflected in the Chat summary.
    suspend fun generateChatSummary(systemExpert: Expert, chat: Chat, previousSummary: ChatSummary?): String {
        ChatUtils.chatUtilsMutex.withLock {
            val cursorNodeId = chat.cursorNodeId ?: return "" // NO CURSOR => Conduit can't do it

            // Unlike history summarization, the cursor itself is included.
            val effectiveHistory = ChatUtils.getEffectiveNodeHistory(chat, cursorNodeId, excludeSystemNodes = true)
            if (effectiveHistory.nodes.size == 0) return ""

            val chatThusFar = AppUtils.getChatContextAsString(
                boundaryContext = effectiveHistory.boundaryContext,
                messages = effectiveHistory.nodes.mapNotNull { it.message },
                maxAssistantTextLen = 250,
                maxUserTextLen = 1500
            )

            val chatSummaryPrompt = PROMPTS.CHAT_SUMMARY_GENERATION
                .replace("{PREVIOUS_SUMMARY}", previousSummary?.summary ?: "NO PREVIOUS SUMMARY EXISTS.")

            Trace.log("CHAT SUMMARY GEN START chat=${chat.id}")

            val result = StringBuilder()
            try {
                systemExpert.getResponse(chatThusFar, chatSummaryPrompt, includeUserModel = false).collect { token ->
                    result.append(token)
                }
            } catch (e: CancellationException) {
                Trace.log("CHAT SUMMARY GEN ABORTED chat=${chat.id}")
                throw e
            }

            Trace.log("CHAT SUMMARY GEN END chat=${chat.id}")
            Trace.log("SUMMARY: new chat summary = $result")

            return result.toString().trim()
        }
    }

    // Generate revised content for the user-model.json file using ONE
    // newly modified chat summary in APPDIR/chats/chat-summaries/*.json
    suspend fun generateUserModel(systemExpert: Expert, newChatSummary: ChatSummary): String {
        if (systemExpert.sessionPtr == null) {
            Trace.log("MAINT: user model skip — system expert unavailable")
            return ""
        }

        val prompt = PROMPTS.USER_MODEL_GENERATION.replace("{NEW_CHAT_SUMMARY}", newChatSummary.summary)

        val result = StringBuilder()
        try {
            systemExpert.getResponse(
                chatThusFar = "THE CURRENT CHAT CONTEXT IS NOT NECESSARY FOR THIS TASK",
                prompt = prompt,  // embedded new chat summary
                includeUserModel = true
            ).collect { token ->
                currentCoroutineContext().ensureActive()
                result.append(token)
            }
        } catch (e: CancellationException) {
            Trace.log("MAINT: USER MODEL GEN ABORTED")
            throw e
        }
        val resultStr = result.toString().trim()

        Trace.log("New user model = $resultStr")
        return resultStr
    }
}
