package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.writeText

interface ChatManager {
    fun getNode(nodeId: String): Node?
    fun newChat(): Chat
    suspend fun addNode(node: Node) : ChatsListItem
    suspend fun updateNode(node: Node) : ChatsListItem

    fun getFullHistory(nodeId: String?): List<Node>
    fun getEffectiveNodeHistory(nodeId: String?): List<Node>

    //fun rollup(nodeId: String, summaryText: String)
    //fun undoRollup(nodeId: String)
}

class LiveChatManager(private val scope: CoroutineScope) : ChatManager {
    private fun createChat(): Chat {
        println("Creating chat") ////
        return Chat.create("Welcome to Conduit")
    }

    var currentChat by mutableStateOf(createChat())

        private set
    @Volatile var isAbortRequested = false
    fun abortCurrentResponse() {
        isAbortRequested = true
    }

    override fun getNode(nodeId: String): Node? {
        return currentChat.nodes[nodeId]
    }

    override fun newChat(): Chat {
        currentChat = createChat()
        return currentChat
    }

    // Called within AppActions:onSend() when a new user or system node is created.
    // It adds a new node (already created) to the convo by 1) adding to the hash of nodes and
    // 2) updating the node's parent to include it amongst its children 3) setting currentNode
    override suspend fun addNode(node: Node): ChatsListItem {
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
            nodes = nodes
        )

        updatedChat.currentLeafNode = node
        currentChat = updatedChat

        return saveChatToDisk(updatedChat)
    }

    override suspend fun updateNode(node: Node): ChatsListItem {
        val chat = currentChat

        val nodes = chat.nodes.toMutableMap()
        nodes.put(node.id, node) ?: throw IllegalStateException("Cannot update node: Node(${node.id}) not found.")

        val updatedChat = chat.copy(
            nodes = nodes
        )

        if (chat.currentLeafNode?.id == node.id) {
            updatedChat.currentLeafNode = node
        } else {
            updatedChat.currentLeafNode = currentChat.currentLeafNode
        }
        currentChat = updatedChat

        return saveChatToDisk(updatedChat)
    }

    // --------------------------------------------------------------------------------------------
    // The following are concerned with saving/loading whole chats from $APPDIR/chats/*.json

    fun loadChatFromDisk(state: AppState, fileName: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val path = Paths.get(getAppPath(), "chats", fileName)
                val jsonString = Files.readString(path)
                val loadedChat = AppJson.decodeFromString<Chat>(jsonString)
                loadedChat.restoreTransients()

                withContext(Dispatchers.Main) {
                    state.chatManager.currentChat = loadedChat
                }
            } catch (e: Exception) {
                println("Failed to load chat: ${e.message}")
            }
        }
    }

    suspend fun saveChatToDisk(chat: Chat): ChatsListItem {
        chat.syncTransients()

        return withContext(Dispatchers.IO) {
            val file = Paths.get(getAppPath(), "chats").resolve(AppUtils.createChatFileName(chat.id, chat.id))

            file.writeText(AppJson.encodeToString(Chat.serializer(), chat))
            val attrs = Files.readAttributes(file, BasicFileAttributes::class.java)

            ChatsListItem(
                title = chat.title,
                fileName = file.fileName.toString(),
                creationTime = attrs.creationTime().toMillis(),
                modificationTime = attrs.lastModifiedTime().toMillis()
            )
        }
    }

//    // sanitize, uniqify
//    fun getChatFileName(chat: Chat): String {
//        val sanitizedTitle = chat.title
//            .take(30)
//            .replace(Regex("[^a-zA-Z0-9]"), "-")
//
//        return "${sanitizedTitle}-${chat.id}.json"
//    }
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
