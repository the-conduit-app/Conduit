package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utilities.conduit.AppUtils.getAppPath
import com.utilities.conduit.ChatUtils.saveChatToDisk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.writeText

class ChatManager(
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()

    var version by mutableIntStateOf(0) // For Compose
        private set

    fun createChat(): Chat = Chat.create("Welcome to Conduit")
    var currentChat by mutableStateOf(createChat())

    @Volatile
    var isAbortRequested = false
        private set

    fun abortCurrentResponse() {
        isAbortRequested = true
    }

    fun clearAbortRequest() {
        isAbortRequested = false
    }

    suspend fun addNode(node: Node): ChatsListItem = mutex.withLock {
        val chat = currentChat

        if (node.parentId != null && !chat.nodes.containsKey(node.parentId)) {
            throw IllegalStateException(
                "Cannot add node: Parent(${node.parentId}) not found."
            )
        }
        chat.nodes[node.id] = node
        node.parentId?.let { parentId ->
            chat.nodes[parentId]?.children?.add(node.id)
        }
        if (chat.rootNodeId == null) {
            chat.rootNodeId = node.id
        }

        chat.currentLeafNode = node
        chat.currentLeafNodeId = node.id

        withContext(Dispatchers.Main) { ++version } // recomp

        ChatUtils.saveChatToDisk(chat)
    }
}
