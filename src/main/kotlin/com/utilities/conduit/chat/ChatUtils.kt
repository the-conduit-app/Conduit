package com.utilities.conduit.chat

import com.utilities.conduit.ui.AppJson
import com.utilities.conduit.AppUtils
import com.utilities.conduit.Expert
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.PROMPTS
import com.utilities.conduit.debug.Trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.writeText


object ChatUtils {
    private val chatUtilsMutex = Mutex() // locking for save etc.

    // N chars from title then uuid
    fun makeChatFileName(chatId: String, chatTitle: String): String {
        val sanitizedTitle = chatTitle
            .take(30)
            .replace(Regex("[^a-zA-Z0-9]"), "-")

        return "${sanitizedTitle}-${chatId}.json"
    }

    // mutexed, writes to disk and blocks until finished
    suspend fun saveChatToDisk(chat: Chat): ChatsListItem {
        chatUtilsMutex.withLock {
            return withContext(Dispatchers.IO) {
                val file = Paths.get(AppUtils.getChatsDir()).resolve(makeChatFileName(chat.id, chat.title))

                file.writeText(AppJson.encodeToString(Chat.serializer(), chat))
                val attrs = Files.readAttributes(file, BasicFileAttributes::class.java)

                ChatsListItem(
                    chat = chat,
                    creationTime = attrs.creationTime().toMillis(),
                    modificationTime = attrs.lastModifiedTime().toMillis()
                )
            }
        }
    }

    // Used to decide whether to present a node as a branching candidate
    // If a node is on the current cursor path is used by both TreeView
    // for rendering, and by the Branching Context menu (check mark etc.)
    fun leadsToCursor(chat: Chat, node: Node): Boolean {
        var currentId = chat.cursorNodeId

        while (currentId != null) {
            if (currentId == node.id) {
                return true
            }
            currentId = chat.nodes[currentId]?.parentId
        }

        return false
    }

    // Only used for views (Text and Tree) and not inference
    // All nodes from root to given node (including the given node)
    fun getFullHistory(chat: Chat, nodeId: String?): List<Node> {
        val historyNodes = mutableListOf<Node>()
        var currentNodeId = nodeId

        while (currentNodeId != null) {
            val node = chat.nodes[currentNodeId] ?: break
            historyNodes.add(node)
            currentNodeId = node.parentId
        }
        return historyNodes.reversed()
    }

    // Same as above, but stops upward traversal at any encountered node that has
    // a non-null historySummary.
    //
    // Return the unsummarized suffix of the history, starting at the nearest
    // upstream node that has a historySummary (or the root if none exists),
    // through the named node.
    //
    // precedingContext is the summary of everything BEFORE that returned
    // suffix. If no summary exists, it contains the "no previous summary"
    // sentinel.
    //
    // E.g: Suppose B* has a summary in AB'CDE.
    // getEffectiveNodeHistory(E) returns:
    //     { previousSummary = B*.summary, nodes = [B*, C, D, E] }
    //
    // THEREFORE: onSend() and generateHistorySummary() invoke it with the parent node.id

    data class EffectiveHistory(
        val boundaryContext: String,
        val nodes: List<Node>
    )
    fun getEffectiveNodeHistory(chat: Chat, nodeId: String?): EffectiveHistory {
        val historyNodes = mutableListOf<Node>()
        var currentNodeId = nodeId
        var precedingContext = "No previous context exists before this point"

        while (currentNodeId != null) {
            val node = chat.nodes[currentNodeId] ?: break
            historyNodes.add(node)
            node.historySummary?.let {
                precedingContext = it
                break
            }
            currentNodeId = node.parentId
        }
        return EffectiveHistory(
            boundaryContext = precedingContext,
            nodes = historyNodes.reversed()
        )
    }

    // ----------------------------------------------------------------------------
    // Various automatic generation routines and helpers

    // Infer a chat title from its contents (Using SystemExpert). This is invoked by both the
    // maint routine for automatic renaming of default chat names, and by the dialog for rename
    // which features a generate button
    suspend fun generateChatTitle(systemExpert: Expert, chat: Chat): String {
        chatUtilsMutex.withLock {
            val oldTitle = chat.title

            val effectiveHistory: EffectiveHistory = getEffectiveNodeHistory(chat, chat.cursorNodeId)

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
                systemExpert.getResponse(AppUtils.getUserModel(), chatThusFar, titlePrompt).collect { token ->
                    result.append(token)
                }
            } catch (e: CancellationException) {
                Trace.log("TITLE GEN ABORTED for $oldTitle")
                throw e
            }
            val newTitle = result.toString().trim()

            Trace.log("TITLE GEN END ('$oldTitle', '$newTitle'")
            return newTitle.ifEmpty { oldTitle }
        }
    }

    // Infer a summary of all nodes up to a node's parent (excluding the node itself). This is
    // then stored and persisted within the node object in the chat by a maint routine.
    // History summaries are used to compress past context succinctly when we trace upwards
    // from the cursor to build context for a prompt.
    suspend fun generateHistorySummary(systemExpert: Expert, chat: Chat, node: Node): String {
        chatUtilsMutex.withLock {
            val effectiveHistory = getEffectiveNodeHistory(chat, node.parentId)

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
                systemExpert.getResponse(AppUtils.getUserModel(), chatThusFar, summaryPrompt).collect { token ->
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

    // Write a summary of the single named chat into the appropriate JSON file in
    // APPDIR/chats/chat-summaries. Why? The maint routine periodically summarizes
    // chats in the background (to chats/chat-summaries). These summaries are picked up by
    // another background maint job (see next) that updates the current user model
    // APPDIR/user-model.json (expert.seedPrompt and user-model ride on every prompt)
    // Another important note: At each update we summarize the current path from root to cursor.
    // When the cursor changes, the result is the AUGMENTED old+new summary.
    // Because the chat is a branching object, the cursor could change between invocations
    // of this function. Thus, the likelihood of a user visiting a particular branch in
    // a conversation is related to that branch being reflected in the Chat summary.
    suspend fun generateChatSummary(systemExpert: Expert, chat: Chat): String {
        chatUtilsMutex.withLock {
            val chatsDir = Paths.get(AppUtils.getChatsDir())
            val summariesDir = chatsDir.resolve("chat-summaries")
            val summaryFile = summariesDir.resolve("${chat.id}.json")

            // Existing summary for this chat-id?
            // NOTE - null will be substituted by "NO PREV SUMMARY EXISTS" downstream in this fun
            val previousChatSummary: String? = withContext(Dispatchers.IO) {
                if (Files.exists(summaryFile)) {
                    try {
                        val chatSummary = AppJson.decodeFromString<ChatSummary>(Files.readString(summaryFile))
                        chatSummary.summary
                    } catch (e: Exception) {
                        Trace.log("CHAT SUMMARY GEN: unable to read existing summary ${summaryFile.fileName}: ${e.message}")
                        null
                    }
                } else {
                    null
                }
            }

            val cursorNodeId = chat.cursorNodeId ?: return "" // NO CURSOR => Conduit can't do it

            // Unlike history summarization, the cursor itself is included.
            val effectiveHistory = getEffectiveNodeHistory(chat, cursorNodeId)
            val chatThusFar = AppUtils.getChatContextAsString(
                boundaryContext = effectiveHistory.boundaryContext,
                messages = effectiveHistory.nodes.mapNotNull { it.message },
                maxAssistantTextLen = 250,
                maxUserTextLen = 1500
            )

            val chatSummaryPrompt = PROMPTS.CHAT_SUMMARY_GENERATION
                .replace("{PREVIOUS_SUMMARY}", previousChatSummary ?: "NO PREVIOUS SUMMARY EXISTS.")

            Trace.log("CHAT SUMMARY GEN START chat=${chat.id}")
            Trace.log(" userModel = userModel\n\nchatThusFar = $chatThusFar\n\nchatSummaryPrompt = $chatSummaryPrompt")

            val result = StringBuilder()
            try {
                systemExpert.getResponse(AppUtils.getUserModel(), chatThusFar, chatSummaryPrompt).collect { token ->
                    result.append(token)
                }
            } catch (e: CancellationException) {
                Trace.log("CHAT SUMMARY GEN ABORTED chat=${chat.id}")
                throw e
            }

            Trace.log("CHAT SUMMARY GEN END chat=${chat.id}")
            Trace.log("SUMMARY Result = $result")

            return result.toString().trim()
        }
    }
}
