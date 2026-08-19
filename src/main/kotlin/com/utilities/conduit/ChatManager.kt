package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ChatManager(
    private val scope: CoroutineScope,
    val systemExpert: Expert)
{
    private val mutex = Mutex()
    var currentGenerationJob: Job? = null

    var version by mutableIntStateOf(0) // For Compose
        private set
    var nodeAddedVersion by mutableIntStateOf(0) // Similar but for autoscroll on addNode
        private set

    var currentChat by mutableStateOf(createChat())

    var currentlyGeneratingExpert: Expert? by mutableStateOf(null)
    val isGenerating: Boolean
        get() = currentlyGeneratingExpert != null

    fun createChat(): Chat = Chat.create("Welcome to Conduit")

    fun beginCurrentResponse(expert: Expert) {
        currentlyGeneratingExpert = expert
    }

    fun finishCurrentResponse() {
        currentlyGeneratingExpert = null
        currentGenerationJob = null
    }

    fun abortCurrentResponse() {
        currentlyGeneratingExpert?.abortResponse()
    }

    suspend fun stopGeneration() {
        Trace.log("CHAT: STOPPING generation job=$currentGenerationJob expert=$currentlyGeneratingExpert")
        currentlyGeneratingExpert?.abortResponse()
        Trace.log("CHAT: ABORT SENT")
        currentGenerationJob?.join()
        Trace.log("CHAT: GENERATION JOINED")
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

        chat.cursorNodeId = node.id

        withContext(Dispatchers.Main) {
            ++version
            ++nodeAddedVersion
        } // recomp

        ChatUtils.saveChatToDisk(chat)
    }

    // When a branch has been selected, the next leaf node is the first node downstream the branch that has
    // either no children (i.e. a leaf) or a non-null summary (i.e. another branchable)
    suspend fun selectBranch(node: Node) {
        var nodeIter = node

        // if node has a child, race down until either a leaf or a branching point
        while (nodeIter.children.size == 1) {
            nodeIter = currentChat.nodes[nodeIter.children[0]]
                ?: return
        }

        currentChat.cursorNodeId = nodeIter.id
        setCursor(nodeIter)
    }

    suspend fun cycleBranch(node: Node) {
        val childNodes = node.children.mapNotNull { currentChat.nodes[it] }
        if (childNodes.isEmpty()) return

        // Find which child currently contains the cursor.
        val currentIndex = childNodes.indexOfFirst {
            ChatUtils.leadsToCursor(currentChat, it)
        }
        val nextIndex = if (currentIndex == -1)  0 else (currentIndex + 1) % childNodes.size

        selectBranch(childNodes[nextIndex])
    }

    suspend fun setCursor(node: Node) {
        currentChat.cursorNodeId = node.id
        ++version // recompose ChatView
        withContext(Dispatchers.IO) { ChatUtils.saveChatToDisk(currentChat) }
    }
}
