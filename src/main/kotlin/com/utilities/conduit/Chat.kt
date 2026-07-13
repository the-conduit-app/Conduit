package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
enum class NodeType { TEXT, INFO }

@Serializable
data class Chat(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val nodes: Map<String, Node> = emptyMap(),
    val rootNodeId: String?,
    val currentLeafNodeId: String? = null,
    ////val bookmarks: List<Bookmark> = emptyList()
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
}

@Serializable
data class MessageAuthor(
    val type: AuthorType,
    val expertId: String? = null,
    val packId: String? = null
)
enum class AuthorType { USER, CONDUIT, EXPERT }

@Serializable
data class ChatMessage(
    val author: MessageAuthor,
    val text: String,

    // Preferentially displayed over text
    @kotlinx.serialization.Transient
    val textInProgress: MutableState<String?> = mutableStateOf(null),

    val timestamp: Long = System.currentTimeMillis(),
    val responseTime: Long? = null
)

@Serializable
data class Node(
    val id: String,
    val type: NodeType,
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
