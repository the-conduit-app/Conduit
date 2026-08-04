package com.utilities.conduit

import com.utilities.conduit.AppUtils.getChatsDir
import jdk.internal.misc.Blocker.end
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeText

object AppUtils {
    fun getAppPath(): String {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")

        val path = when {
            os.contains("mac") -> "$home/Library/Application Support/Conduit"
            os.contains("win") -> "${System.getenv("APPDATA")}/Conduit"
            else -> "$home/.config/conduit"
        }

        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()
        return path
    }

    fun getNativeLibPath(libName: String): String {
        return "${getAppPath()}/lib/$libName"
    }

    fun getPacksDir(): String {
        return "${getAppPath()}/packs"
    }

    fun getChatsDir(): String {
        return "${getAppPath()}/chats"
    }

    fun getModelsDir(): String {
        return "${getAppPath()}/llm"
    }

    fun getAbsolutePathString(path: String): String {
        return if (path.startsWith("/")) path else "${getAppPath()}/$path"
    }

    fun formatDateRange(
        creation: Long,
        modification: Long
    ): String {
        val created = Instant.ofEpochMilli(creation).atZone(ZoneId.systemDefault()).toLocalDate()
        val modified = Instant.ofEpochMilli(modification).atZone(ZoneId.systemDefault()).toLocalDate()

        val fmt = DateTimeFormatter.ofPattern("MMM d")

        return if (created == modified) {
            modified.format(fmt)
        } else {
            "${created.format(fmt)} – ${modified.format(fmt)}"
        }
    }


    // This is used to append the date to a message bubble header for the 1st user prompt
    // of a day (E.g. You - July 32) in any chat (Ref: AppActions:onSend)
    // For the very first bubble in a new chat prevTimeStamp will be null, and it will
    // always get to append the current date. Otherwise, returns "" unless prevTimeStamp
    // was yesterday.
    fun makeOptionalDateTag(prevTimeStamp: Long?): String {
        val now = System.currentTimeMillis()
        val currentDate = kotlin.time.Instant.fromEpochMilliseconds(now)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val prevDate = prevTimeStamp?.let {
            kotlin.time.Instant.fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }

        return if (prevDate != currentDate) {
            " · ${currentDate.day} ${currentDate.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)}"
        } else {
            ""
        }
    }
}

object ChatUtils {
    private val mutex = Mutex() // locking for save etc.

    fun makeChatFileName(chatId: String, chatTitle: String): String {
        val sanitizedTitle = chatTitle
            .take(30)
            .replace(Regex("[^a-zA-Z0-9]"), "-")

        return "${sanitizedTitle}-${chatId}.json"
    }

    suspend fun saveChatToDisk(chat: Chat): ChatsListItem {
        mutex.withLock {
            chat.syncTransients()

            return withContext(Dispatchers.IO) {
                val file = Paths.get(getChatsDir()).resolve(makeChatFileName(chat.id, chat.title))

                file.writeText(AppJson.encodeToString(Chat.serializer(), chat))
                val attrs = Files.readAttributes(file, BasicFileAttributes::class.java)

                ChatsListItem(
                    chat = chat,
                    creationTime = attrs.creationTime().toMillis(),
                    modificationTime = attrs.lastModifiedTime().toMillis()
                )
            }
        }
    }

    // Only used for composing the full chat views (Text and Tree)
    fun getFullHistory(chat: Chat, nodeId: String?): List<Node> {
        val history = mutableListOf<Node>()
        var currentId = nodeId

        while (currentId != null) {
            val node = chat.nodes[currentId] ?: break
            history.add(node)
            currentId = node.parentId
        }
        return history.reversed()
    }

    // Return a list of nodes that make up the history of the current node
    // from THE NEAREST SUMMARIZED UPSTREAM NODE (or root node) to the named node.
    // Only used for inference and not for UI (ignore CONDUIT message nodes).
    fun getEffectiveNodeHistory(chat: Chat, nodeId: String?): List<Node> {
        val history = mutableListOf<Node>()
        var currentId = nodeId

        while (currentId != null) {
            val node = chat.nodes[currentId] ?: break
            val authorType = node.message?.author?.type
            if (authorType == AuthorType.USER || authorType == AuthorType.ASSISTANT) {
                history.add(node)
            }
            if (node.summaryToThisNode != null) {
                break
            }
            currentId = node.parentId
        }
        return history.reversed()
    }

    suspend fun generateChatTitle(systemExpert: Expert, chat: Chat): String {
        val oldTitle : String = chat.title
        val maxTextLen = 250

        var messages = getEffectiveNodeHistory(chat, chat.currentLeafNodeId)
            .mapNotNull { it.message }
            .map { message ->
                if (message.author.type == AuthorType.ASSISTANT && message.text.length > maxTextLen) {
                    message.copy(text = message.text.take(maxTextLen) + "...")
                } else {
                    message
                }
            }

        messages += ChatMessage(
            author = MessageAuthor(type = AuthorType.SYSTEM),
            text = PROMPTS.TITLE_GENERATION
        )

        val result = StringBuilder()
        systemExpert.getResponse(messages).collect { token ->
            result.append(token)
        }

        val newTitle = result.toString().trim()
        return if (newTitle.isNotEmpty()) newTitle else oldTitle
    }
}
