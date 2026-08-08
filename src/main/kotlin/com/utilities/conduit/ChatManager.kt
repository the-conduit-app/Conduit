package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ChatManager(
    private val scope: CoroutineScope,
    val systemExpert: Expert)
{
    private val mutex = Mutex()

    var version by mutableIntStateOf(0) // For Compose
        private set

    var currentChat by mutableStateOf(createChat())

    var currentlyGeneratingExpert: Expert? by mutableStateOf(null)

    fun createChat(): Chat = Chat.create("Welcome to Conduit")

    fun beginCurrentResponse(expert: Expert) {
        currentlyGeneratingExpert = expert
    }

    fun finishCurrentResponse() {
        currentlyGeneratingExpert = null
    }

    fun abortCurrentResponse() {
        println("ABORT 1 ChatManager ${System.currentTimeMillis()}")
        currentlyGeneratingExpert?.abortResponse()
        println("ABORT 2 ChatManager ${System.currentTimeMillis()}")
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

        withContext(Dispatchers.Main) {
            ++version
        } // recomp

        ChatUtils.saveChatToDisk(chat)
    }
}
