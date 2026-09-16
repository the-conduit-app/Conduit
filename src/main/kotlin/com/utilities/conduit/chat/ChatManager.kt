package com.utilities.conduit.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utilities.conduit.Expert
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.utils.ChatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ChatManager {
    private val mutex = Mutex()
    var currentGenerationJob: Job? = null

    var version by mutableIntStateOf(0)     // For general Compose invalidation
        private set
    var chatVersion by mutableIntStateOf(0) // Similar but for autoscroll on addNode
        private set

    var currentChat by mutableStateOf(createChat())

    // Compose observable state of streaming messages, which take precedence over a node.message.text
    private val textInProgress = mutableStateMapOf<String, String>()

    var currentlyGeneratingExpert: Expert? by mutableStateOf(null)

    // IMPORTANT NOTE ABOUT THIS VAR:
    // It is ONLY set for explicit user generate requests (onSend, etc.).
    // It is NOT set for internal SystemExpert requests.
    // Auto-generation is cancellable, while user-initiated requests
    // (rename suggestions, response requests, etc.) are blocking.
    // Therefore, this variable can safely be observed for node animation
    // in TreeView.
    val isGenerating: Boolean
        get() = currentlyGeneratingExpert != null

    fun createChat(): Chat = Chat.create("Welcome to Conduit")

    fun onBeginCurrentResponse(expert: Expert) {
        currentlyGeneratingExpert = expert
    }

    fun onFinishCurrentResponse() {
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

    fun getTextInProgress(nodeId: String): String? =
        textInProgress[nodeId]

    fun setTextInProgress(nodeId: String, text: String) {
        textInProgress[nodeId] = text
    }

    fun clearTextInProgress(nodeId: String) {
        textInProgress.remove(nodeId)
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
            ++chatVersion // forces autoscroll in chatView
            ++version
        }

        ChatUtils.saveChatToDisk(chat)
    }

//    // compressChildren and its private helper removeNode() are used as follows. If we try to add a response
//    // leaf to a parent (userNode) with an existing child having identical text, we toss the new
//    // responseNode from the chat and set the cursor to the original child. This allows us to navigate
//    // a tree - mainly intended for Conduit Quest Trails, so people can traverse a trail and retry advancing
//    // from a previous "stuck" place.
//    suspend fun compressChildren(userNode: Node, responseNode: Node): Node {
//        val chat = currentChat
//
//        ////
//        val children = userNode.children.mapNotNull { chat.nodes[it] }
//        println("COMPRESS response=${responseNode.id} text='${responseNode.message?.text}'")
//        println("COMPRESS children=${children.map { "${it.id}: '${it.message?.text}'" }}")
//        ////
//
//        val duplicateNode = userNode.children
//            .mapNotNull { chat.nodes[it] }
//            .firstOrNull { node ->
//                node.id != responseNode.id && node.message?.text == responseNode.message?.text
//            }
//        println("COMPRESS duplicate=${duplicateNode?.id}")////
//
//        if (duplicateNode != null) {
//            chat.cursorNodeId = duplicateNode.id
//            println("COMPRESS remove=${removeNode(responseNode)}")
//        }
//
////        if (duplicateNode != null && removeNode(responseNode)) {
////            chat.cursorNodeId = duplicateNode.id
////            //++version
////        }////
//
//        return duplicateNode ?: responseNode
//    }
//    private fun removeNode(node: Node): Boolean {
//        if (node.children.isNotEmpty()) return false
//
//        val parent = node.parentId?.let { currentChat.nodes[it] } ?: return false
//
//        parent.children.remove(node.id)
//        currentChat.nodes.remove(node.id)
//        ++treeVersion
//
//        return true
//    }

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
