package com.utilities.conduit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface ChatManager {
    fun getNode(nodeId: String): Node?
    fun addNode(node: Node)
    fun updateNode(node: Node)

    fun getFullHistory(nodeId: String?): List<Node>
    fun getEffectiveNodeHistory(nodeId: String?): List<Node>

    //fun rollup(nodeId: String, summaryText: String)
    //fun undoRollup(nodeId: String)
}

class LiveChatManager(initialChat: Chat) : ChatManager {
    var currentChat = mutableStateOf(initialChat)

    // This is used to disable SEND while a current response is streaming
    var isStreaming by mutableStateOf(false)

    override fun getNode(nodeId: String): Node? {
        return currentChat.value.nodes[nodeId]
    }

    // Adds a new node (already created) to the convo by 1) adding to the hash of nodes and
    // 2) updating the node's parent to include it amongst its children 3) setting currentNode
    override fun addNode(node: Node) {
        // 1. Access the underlying Chat object
        val chat = currentChat.value
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

        currentChat.value = chat.copy(
            rootNodeId = chat.rootNodeId ?: node.id,
            currentLeafNodeId = node.id,
            nodes = nodes
        )
    }

    override fun updateNode(node: Node) {
        val nodes = currentChat.value.nodes.toMutableMap()
        nodes.apply { put(node.id, node) }
        currentChat.value = currentChat.value.copy(nodes = nodes)
    }

    // Only used in the UI layout for indiv chat bubbles
    override fun getFullHistory(nodeId: String?): List<Node> {
        val history = mutableListOf<Node>()
        var currentId = nodeId

        while (currentId != null) {
            val node = currentChat.value.nodes[currentId] ?: break
            history.add(node)
            currentId = node.parentId
        }
        return history.reversed()
    }

    // Return a list of nodes that make up the history of the current node
    // from THE NEAREST SUMMARIZED UPSTREAM NODE (or root node) to the named node.
    // Only used for inference and not for UI
    override fun getEffectiveNodeHistory(nodeId: String?): List<Node> {
        val history = mutableListOf<Node>()
        var currentId = nodeId

        while (currentId != null) {
            val node = currentChat.value.nodes[currentId] ?: break

            if (node.summaryToThisNode != null) {
                val summaryMessage = ChatMessage(author = MessageAuthor(type = AuthorType.CONDUIT), text = node.summaryToThisNode)
                history.add(node.copy(message = summaryMessage))
                break
            }

            history.add(node)
            currentId = node.parentId
        }
        return history.reversed()
    }
}
