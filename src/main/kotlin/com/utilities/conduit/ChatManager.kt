package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.exists
import kotlin.io.path.writeText

interface ChatManager {
    fun getNode(nodeId: String): Node?
    fun addNode(node: Node)
    fun updateNode(node: Node)

    fun getFullHistory(nodeId: String?): List<Node>
    fun getEffectiveNodeHistory(nodeId: String?): List<Node>

    //fun rollup(nodeId: String, summaryText: String)
    //fun undoRollup(nodeId: String)
}

class LiveChatManager(private val scope: CoroutineScope, initialChat: Chat) : ChatManager {
    var currentChat by mutableStateOf(initialChat)

    // This is used to disable SEND while a current response is streaming
    // TODO - check if still relevant
    var isStreaming by mutableStateOf(false)

    override fun getNode(nodeId: String): Node? {
        return currentChat.nodes[nodeId]
    }

    // Adds a new node (already created) to the convo by 1) adding to the hash of nodes and
    // 2) updating the node's parent to include it amongst its children 3) setting currentNode
    override fun addNode(node: Node) {
        val chat = currentChat
        val nodes = chat.nodes.toMutableMap()

        if (node.parentId != null && !chat.nodes.containsKey(node.parentId)) {
            throw IllegalStateException("Cannot add node: Parent(${node.parentId}) not found.")
        }

        nodes[node.id] = node
        node.parentId?.let {
            nodes.computeIfPresent(it) { _, parent ->
                parent.copy(children = parent.children + node.id)
            }
        }

        val updatedChat = chat.copy(
            rootNodeId = chat.rootNodeId ?: node.id,
            currentLeafNodeId = node.id,
            nodes = nodes
        )
        currentChat = updatedChat

        scope.launch(Dispatchers.IO) {
            saveChatToDisk(updatedChat)
        }
    }

    override fun updateNode(node: Node) {
        val nodes = currentChat.nodes.toMutableMap()
        nodes.apply { put(node.id, node) }
        currentChat = currentChat.copy(nodes = nodes)
        scope.launch(Dispatchers.IO) {
            saveChatToDisk(currentChat)
        }
    }

    // --------------------------------------------------------------------------------------------
    // The following are concerned with saving/loading whole chats from $APPDIR/chats/*.json

    fun loadChatFromDisk(state: AppState, chatId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val path = java.nio.file.Paths.get(getAppPath(), "chats", "$chatId.json")
                val jsonString = java.nio.file.Files.readString(path)
                val loadedChat = AppJson.decodeFromString<Chat>(jsonString)

                withContext(Dispatchers.Main) {
                    state.chatManager.currentChat = loadedChat
                }
            } catch (e: Exception) {
                println("Failed to load chat: ${e.message}")
            }
        }
    }
    suspend fun saveChatToDisk(chat: Chat) {
        withContext(Dispatchers.IO) {
            val file = getOrCreateFile(chat)
            file.writeText(AppJson.encodeToString(Chat.serializer(), chat))
        }
    }

    private fun getOrCreateFile(chat: Chat): java.nio.file.Path {
        val chatDir = java.nio.file.Paths.get(getAppPath(), "chats")

        if (!java.nio.file.Files.exists(chatDir)) {
            java.nio.file.Files.createDirectories(chatDir)
        }

        val targetPath = chatDir.resolve("${chat.id}.json")
        if (java.nio.file.Files.exists(targetPath)) { return targetPath }

        val sanitized = chat.title.take(30).replace(Regex("[^a-zA-Z0-9]"), "-")
        return chatDir.resolve("${sanitized}-${chat.id.take(2)}.json")
    }
    //-----------------------------------------------------------------------------------------------------

    // Only used for composing the full chat views (Text and Tree)
    override fun getFullHistory(nodeId: String?): List<Node> {
        val history = mutableListOf<Node>()
        var currentId = nodeId

        while (currentId != null) {
            val node = currentChat.nodes[currentId] ?: break
            history.add(node)
            currentId = node.parentId
        }
        return history.reversed()
    }

    // Return a list of nodes that make up the history of the current node
    // from THE NEAREST SUMMARIZED UPSTREAM NODE (or root node) to the named node.
    // Only used for inference and not for UI (ignore CONDUIT message nodes).
    override fun getEffectiveNodeHistory(nodeId: String?): List<Node> {
        val history = mutableListOf<Node>()
        var currentId = nodeId

        while (currentId != null) {
            val node = currentChat.nodes[currentId] ?: break
            val authorType = node.message?.author?.type
            if (authorType == AuthorType.USER || authorType == AuthorType.ASSISTANT) {
                history.add(node)
            }
            if (node.summaryToThisNode != null) {
                break
            }
            currentId = node.parentId
        }
        return history.reversed()
    }
}
