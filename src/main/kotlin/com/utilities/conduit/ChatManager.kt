package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val isGenerating: Boolean
        get() = currentlyGeneratingExpert != null

    fun createChat(): Chat = Chat.create("Welcome to Conduit")

    fun beginCurrentResponse(expert: Expert) {
        currentlyGeneratingExpert = expert
    }

    fun finishCurrentResponse() {
        currentlyGeneratingExpert = null
    }

    fun abortCurrentResponse() {
        currentlyGeneratingExpert?.abortResponse()
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

    // When a node has <= 2 branches, clicking on the branch icon arrow doesn't
    // bring up the branch selection menu, but immediately switches to the only other branch
    suspend fun selectOtherBranch(node: Node) {
        var nodeIter = node.children
            .mapNotNull { currentChat.nodes[it] }
            .firstOrNull { !ChatUtils.leadsToCursor(currentChat, it) }
            ?: return

        while (true) {
            if (nodeIter.children.size != 1) {
                setCursor(nodeIter)
                return
            }

            nodeIter = currentChat.nodes[nodeIter.children[0]]
                ?: return
        }
    }

    suspend fun setCursor(node: Node) {
        currentChat.cursorNodeId = node.id
        ++version // recompose ChatView
        withContext(Dispatchers.IO) { ChatUtils.saveChatToDisk(currentChat) }
    }
}
