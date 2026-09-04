package com.utilities.conduit.utils

import com.utilities.conduit.Expert
import com.utilities.conduit.ExpertType
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.Chat
import com.utilities.conduit.chat.ChatsListItem
import com.utilities.conduit.chat.Node
import com.utilities.conduit.ui.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.writeText

object ChatUtils {
    // Can't be private because accessed from MaintUtils
    val chatUtilsMutex = Mutex() // locking for save etc.

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

                file.writeText(AppJson.encodeToString(Chat.Companion.serializer(), chat))
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
    fun getEffectiveNodeHistory(chat: Chat, nodeId: String?, excludeSystemNodes: Boolean = false): EffectiveHistory {
        val historyNodes = mutableListOf<Node>()
        var currentNodeId = nodeId
        var precedingContext = "No previous context exists before this point"

        while (currentNodeId != null) {
            val node = chat.nodes[currentNodeId] ?: break

            // SYSTEM nodes represent internal expert responses.
            // Since traversal is leaf → root, also skip the user node
            // that immediately precedes this response.
            if (excludeSystemNodes && node.message?.author?.type == AuthorType.SYSTEM) {
                currentNodeId = node.parentId?.let { chat.nodes[it]?.parentId }
                continue
            }

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
}
