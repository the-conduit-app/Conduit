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

// TODO...Don't delete this line until place below better
enum class ResponseGenerationStatus { IDLE, GENERATING, ABORTING }

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
        scope.launch(Dispatchers.Main) { responseStatus = ResponseGenerationStatus.GENERATING }
    }
    fun finishCurrentResponse() {
        scope.launch(Dispatchers.Main) { responseStatus = ResponseGenerationStatus.IDLE }
    }
    fun abortCurrentResponse() {
        println("ABORT 1 ChatManager ${System.currentTimeMillis()}")
        currentlyGeneratingExpert?.abortResponse()
        println("ABORT 2 ChatManager ${System.currentTimeMillis()}")

        scope.launch(Dispatchers.Main) { responseStatus = ResponseGenerationStatus.ABORTING }
    }

    var responseStatus by mutableStateOf(ResponseGenerationStatus.IDLE)
        private set

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

        // Check if to be renamed
        if (chat.title.equals("Welcome to Conduit") && chat.nodes.size > 2 && chat.renameStatus == ChatRenameStatus.NONE) {
            chat.renameStatus = ChatRenameStatus.GENERATING
            scope.launch(Dispatchers.IO) {
                val newTitle = ChatUtils.generateChatTitle(systemExpert, chat)
                withContext(Dispatchers.Main) {
                    chat.title = newTitle
                    chat.renameStatus = ChatRenameStatus.DONE
                }
            }
        }
        ChatUtils.saveChatToDisk(chat)
    }
}
