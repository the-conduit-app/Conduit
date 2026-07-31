package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class Chat(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val nodes: Map<String, Node> = emptyMap(),
    val rootNodeId: String?,
    var currentLeafNodeId: String? = null,

    @Transient
    var currentLeafNode: Node? = null
) {
    companion object {
        fun create(title: String): Chat {
            return Chat(
                id = UUID.randomUUID().toString(),
                title = title,
                rootNodeId = null
            )
        }
    }
    fun restoreTransients() {
        currentLeafNode = currentLeafNodeId?.let(nodes::get)
    }
    fun syncTransients() {
        currentLeafNodeId = currentLeafNode?.id
    }
}

@Serializable
data class Node(
    val id: String,
    val type: NodeType,
    val createdAt: Long = System.currentTimeMillis(),
    val parentId: String?,
    val children: List<String> = emptyList(),

    val message: ChatMessage?, // Payload
    val summaryToThisNode: String? = null
) {
    companion object {
        fun create(
            type: NodeType,
            parentId: String?,
            message: ChatMessage? = null
        ): Node {
            return Node(
                id = UUID.randomUUID().toString(),
                type = type,
                parentId = parentId,
                message = message
            )
        }
    }
}
@Serializable
enum class NodeType { TEXT, INFO }

@Serializable
data class ChatMessage(
    val title: String? = null,
    val author: MessageAuthor,

    // Canonical text saved to disk.
    var text: String,

    var status: MessageStatus = MessageStatus.COMPLETE,

    val timestamp: Long = System.currentTimeMillis(),
    val responseTime: Long? = null
) {
    // Run-time only states

    // Displayed preferentially to text while the message is streaming.
    @Transient
    val textInProgress: MutableState<String?> = mutableStateOf(null)

    val isStreaming: Boolean
        get() = textInProgress.value != null
}
enum class MessageStatus {
    COMPLETE,
    INTERRUPTED,
    ERROR
}

@Serializable
data class MessageAuthor(
    val type: AuthorType,
    val expertId: String? = null,
    val packId: String? = null
)
enum class AuthorType {
    USER,
    ASSISTANT,
    CONDUIT,
}
