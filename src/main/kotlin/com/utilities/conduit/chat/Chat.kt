package com.utilities.conduit.chat

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class Chat(
    val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    var rootNodeId: String?,
    var cursorNodeId: String? = null,
    var title: String,
    var needsHumanReview: Boolean = false,
    val nodes: MutableMap<String, Node> = mutableMapOf(),

    @Transient
    var renameStatus: ChatRenameStatus = ChatRenameStatus.NONE,
    ) {
    //var isGenerating by mutableStateOf(false)

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

// Used for automatic chat renaming
enum class ChatRenameStatus { NONE, GENERATING, DONE }

// ---------------------------------------------------------------------------
@Serializable
data class Node(
    val id: String,
    val type: NodeType,
    val createdAt: Long = System.currentTimeMillis(),
    val parentId: String?,
    val children: MutableList<String> = mutableListOf(),

    val message: ChatMessage?, // Payload
    var historySummary: String? = null
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

// ---------------------------------------------------------------------------

// NOTE: The nick name of the expert and the name of the pack from which this expert came
// are the ONLY things persisted on disk. So assume that a reader of this class gets
// a nickname and a pack name separated by a dot. This "title" is manufactured and set in
// onSend() which is the way users send stuff into Conduit.
@Serializable
data class ChatMessage(
    var title: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var status: MessageStatus = MessageStatus.COMPLETE,

    val author: MessageAuthor,
    var text: String, // Canonical text saved to disk.
    var responseTime: Long? = null
) {
    // Run-time only states

    // Displayed preferentially to text while the message is streaming.
    @Transient
    val textInProgress: MutableState<String?> = mutableStateOf(null)
}
enum class MessageStatus {
    COMPLETE,
    INTERRUPTED,
    ERROR
}

// ---------------------------------------------------------------------------

@Serializable
data class MessageAuthor(
    val type: AuthorType,
    val expertId: String? = null,
    val packId: String? = null
)
enum class AuthorType {
    USER,
    ASSISTANT,
    SYSTEM,   // Responses from INTERNAL experts are tagged SYSTEM
}
