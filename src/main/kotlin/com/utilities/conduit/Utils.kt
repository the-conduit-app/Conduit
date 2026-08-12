package com.utilities.conduit

import com.utilities.conduit.AppUtils.getChatsDir
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
import kotlin.coroutines.cancellation.CancellationException
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

    fun getAbsoluteModelPath(modelPath: String): String {
            return if (Paths.get(modelPath).isAbsolute)
                modelPath
            else
                Paths.get(getAppPath(), modelPath).toString()
    }


    /**
     * Reads all .json files in the packs directory and parses them into Pack objects
     * IMPORTANT: NO PACK EXPERT INITIALIZATIONS (Hence not time-consuming)
     */
    suspend fun getAvailablePacks(): List<Pack> = withContext(Dispatchers.IO) {
        val packsDir = Paths.get(getAppPath(), "packs")

        if (!Files.exists(packsDir))
            return@withContext emptyList()

        Files.list(packsDir).use { stream ->
            stream.toList()
                .sortedBy { it.fileName.toString() }
                .filter { it.toString().endsWith(".json") }
                .mapNotNull { path ->
                    try {
                        val id = path.fileName.toString().removeSuffix(".json")
                        val json = Files.readString(path)
                        AppJson.decodeFromString<Pack>(json).copy(id = id)
                    } catch (e: Exception) {
                        println("Error loading pack ${path.fileName}: ${e.message}")
                        null
                    }
                }
        }
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
    private val chatUtilsMutex = Mutex() // locking for save etc.

    fun makeChatFileName(chatId: String, chatTitle: String): String {
        val sanitizedTitle = chatTitle
            .take(30)
            .replace(Regex("[^a-zA-Z0-9]"), "-")

        return "${sanitizedTitle}-${chatId}.json"
    }

    suspend fun saveChatToDisk(chat: Chat): ChatsListItem {
        chatUtilsMutex.withLock {
            //chat.syncTransients()

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
            if (node.historySummary != null) {
                break
            }
            currentId = node.parentId
        }
        return history.reversed()
    }

    suspend fun generateChatTitle(systemExpert: Expert, chat: Chat): String {
        chatUtilsMutex.withLock {
            val oldTitle : String = chat.title
            val maxTextLen = 250

            if (systemExpert.sessionPtr == null) {
                error("Generating chat title for ${chat.title} return early (sysExpert = ${systemExpert.sessionPtr})")
            }

            var messages = getEffectiveNodeHistory(chat, chat.cursorNodeId)
                .mapNotNull { it.message }
                .map { message ->
                    if (message.author.type == AuthorType.ASSISTANT && message.text.length > maxTextLen) {
                        message.copy(text = message.text.take(maxTextLen) + "...")
                    } else {
                        message
                    }
                }

            val currentTitle = if (oldTitle.equals("Welcome to Conduit", ignoreCase = true))
                "NO CURRENT TITLE"
            else
                oldTitle

            val prompt = PROMPTS.TITLE_GENERATION.replace("{CURRENT_TITLE}", currentTitle)
            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.SYSTEM),
                text = prompt
            )

            Trace.log("TITLE GEN START session=${systemExpert.sessionPtr}")
            val result = StringBuilder()
            systemExpert.getResponse(messages).collect { token ->
                result.append(token)
            }
            Trace.log("TITLE GEN END session=${systemExpert.sessionPtr}")

            val newTitle = result.toString().trim()
            return newTitle.ifEmpty { oldTitle }
        }
    }

    suspend fun generateHistorySummary(systemExpert: Expert, chat: Chat, node: Node): String {
        chatUtilsMutex.withLock {
            val history = getEffectiveNodeHistory(chat, node.id) + node

            val previousSummary =
                history.firstOrNull()?.historySummary.orEmpty()

            val conversation = history
                .mapNotNull { it.message }
                .joinToString("\n\n") { message ->
                    "${message.author.type}: ${message.text}"
                }

            val prompt = PROMPTS.HISTORY_SUMMARY_GENERATION
                .replace("{PREVIOUS_SUMMARY}", previousSummary)
                .replace("{CONVERSATION}", conversation)

            val messages = listOf(
                ChatMessage(
                    author = MessageAuthor(type = AuthorType.SYSTEM),
                    text = prompt
                )
            )

            Trace.log("HISTORY SUMMARY START session=${systemExpert.sessionPtr}")

            val result = StringBuilder()
            try {
                systemExpert.getResponse(messages).collect { token ->
                    result.append(token)
                }
                Trace.log("HISTORY SUMMARY END session=${systemExpert.sessionPtr}")
            } catch (e: CancellationException) {
                Trace.log("HISTORY SUMMARY ABORTED session=${systemExpert.sessionPtr}")
                throw e
            }

            Trace.log("HISTORY SUMMARY END session=${systemExpert.sessionPtr}")

            return result.toString().trim()
        }
    }

    // Used to decide whether to present a node as a branching candidate
    fun leadsToCursor(chat: Chat, node: Node): Boolean {
        var currentId = chat.cursorNodeId

        while (currentId != null) {
            if (currentId == node.id) {
                return true
            }
            currentId = chat.nodes[currentId]?.parentId
        }

        return false
    }
}
