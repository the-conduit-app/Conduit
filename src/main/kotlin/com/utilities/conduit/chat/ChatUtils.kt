package com.utilities.conduit.chat

import com.utilities.conduit.ui.AppJson
import com.utilities.conduit.AppUtils
import com.utilities.conduit.Expert
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.PROMPTS
import com.utilities.conduit.debug.Trace
import kotlinx.coroutines.Dispatchers
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

    fun makeChatFileName(chatId: String, chatTitle: String): String {
        val sanitizedTitle = chatTitle
            .take(30)
            .replace(Regex("[^a-zA-Z0-9]"), "-")

        return "${sanitizedTitle}-${chatId}.json"
    }

    suspend fun saveChatToDisk(chat: Chat): ChatsListItem {
        chatUtilsMutex.withLock {
            //chat.syncTransients()

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

    // Only used for composing the full chat views (Text and Tree)
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
    // previousSummary is the summary of everything BEFORE that returned
    // suffix. If no summary exists, it contains the "no previous summary"
    // sentinel.
    //
    // E.g: Suppose B' has a summary in AB'CDE.
    // getEffectiveNodeHistory(E) returns:
    //     { previousSummary = B'.summary, nodes = [B', C, D, E] }

    data class EffectiveHistory(
        val precedingContext: String,
        val nodes: List<Node>
    )
    fun getEffectiveNodeHistory(chat: Chat, nodeId: String?): EffectiveHistory {
        val historyNodes = mutableListOf<Node>()
        var currentNodeId = nodeId
        var previousSummary = "No previous context exists before this point"

        while (currentNodeId != null) {
            val node = chat.nodes[currentNodeId] ?: break
            historyNodes.add(node)
            node.historySummary?.let {
                previousSummary = it
                break
            }
            currentNodeId = node.parentId
        }
        return EffectiveHistory(
            precedingContext = previousSummary,
            nodes = historyNodes.reversed()
        )
    }

    suspend fun generateChatTitle(systemExpert: Expert, chat: Chat): String {
        chatUtilsMutex.withLock {
            val oldTitle = chat.title
            val maxTextLen = 250
            val sessionPtr = systemExpert.sessionPtr ?: error(
                "Generating chat title for ${chat.title} return early (sysExpert = ${systemExpert.sessionPtr})"
            )
            val effectiveHistory = getEffectiveNodeHistory(chat, chat.cursorNodeId)
            val messages = effectiveHistory.nodes
                .mapNotNull { it.message }
                .map { message ->
                    if (
                        message.author.type == AuthorType.ASSISTANT &&
                        message.text.length > maxTextLen
                    ) {
                        message.copy(text = message.text.take(maxTextLen) + "...")
                    } else {
                        message
                    }
                }
                .toMutableList()

            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.USER),
                text = PROMPTS.TITLE_GENERATION
            )

            val prompt = AppUtils.buildPrompt(systemExpert, messages)
                .replace("{CURRENT_TITLE}", oldTitle)
                .replace("{PRECEDING_CONTEXT}", effectiveHistory.precedingContext)

            Trace.log("TITLE GEN START session=$sessionPtr")

            val result = StringBuilder()
            LlmPortal.getResponse(sessionPtr, prompt).collect { token ->
                result.append(token)
            }

            Trace.log("TITLE GEN END session=$sessionPtr")

            val newTitle = result.toString().trim()
            return newTitle.ifEmpty { oldTitle }
        }
    }

    suspend fun generateHistorySummary(
        systemExpert: Expert,
        chat: Chat,
        node: Node
    ): String {
        chatUtilsMutex.withLock {
            val sessionPtr = systemExpert.sessionPtr
                ?: error(
                    "Generating history summary for ${chat.title} return early " +
                            "(sysExpert = ${systemExpert.sessionPtr})"
                )

            val effectiveHistory = getEffectiveNodeHistory(chat, node.id)

            val messages = mutableListOf<ChatMessage>()

            // Context summarized before the nodes below.
            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.USER),
                text = "Preceding context:\n${effectiveHistory.precedingContext}"
            )

            // The summary for `node` is up to, but does not include, `node`.
            messages += effectiveHistory.nodes
                .dropLast(1)
                .mapNotNull { it.message }

            // Tell the model what to do after presenting the context/conversation.
            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.USER),
                text = PROMPTS.HISTORY_SUMMARY_GENERATION
            )

            val prompt = AppUtils.buildPrompt(systemExpert, messages)

            Trace.log(
                "MAINT: HISTORY SUMMARY START session=$sessionPtr node=${node.id}"
            )

            val result = StringBuilder()
            try {
                LlmPortal.getResponse(sessionPtr, prompt).collect { token ->
                    result.append(token)
                }
            } catch (e: CancellationException) {
                Trace.log("MAINT: HISTORY SUMMARY ABORTED node=${node.id}")
                throw e
            }

            Trace.log(
                "MAINT: HISTORY SUMMARY END session=$sessionPtr node=${node.id}"
            )

            return result.toString().trim()
        }
    }

    // Used to decide whether to present a node as a branching candidate
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
}
