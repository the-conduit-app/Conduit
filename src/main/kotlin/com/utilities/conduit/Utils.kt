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

    // Returns a prompt wrapped in ChatML
    fun buildPrompt(expert: Expert, messages: List<ChatMessage>): String {
        return buildString {

            expert.seedPrompt
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append("<|im_start|>user\n")
                    append(it)
                    append("\n<|im_end|>\n")
                }

            messages.forEach { msg ->
                val role = when (msg.author.type) {
                    AuthorType.USER      -> "user"
                    AuthorType.ASSISTANT -> "assistant"
                    AuthorType.SYSTEM    -> "system"
                }

                append("<|im_start|>")
                append(role)
                append('\n')

                if (msg.author.type == AuthorType.ASSISTANT) {
                    val nickname = msg.title?.substringBefore("·")?.trim()
                    if (!nickname.isNullOrEmpty())
                        append("${nickname}: ")
                }

                append(msg.text)
                append("\n<|im_end|>\n")
            }

            append("<|im_start|>assistant\n")
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

// -------------------------------------------------------------------------------------------

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
    // All nodes from root to given node (including the given node)
    fun getFullHistory(chat: Chat, nodeId: String?): List<Node> {
        val historyNodes = mutableListOf<Node>()
        var currentNodeId = nodeId

        while (currentNodeId != null) {
            val node = chat.nodes[currentNodeId] ?: break
            historyNodes.add(node)
            currentNodeId = node.parentId
        }
        return historyNodes.reversed()
    }

    // Same as above, but stops upward traversal at any encountered node that has
    // a non-null historySummary.
    //
    // Return the unsummarized suffix of the history, starting at the nearest
    // upstream node that has a historySummary (or the root if none exists),
    // through the named node.
    //
    // previousSummary is the summary of everything BEFORE that returned
    // suffix. If no summary exists, it contains the "no previous summary"
    // sentinel.
    //
    // E.g: Suppose B' has a summary in AB'CDE.
    // getEffectiveNodeHistory(E) returns:
    //     { previousSummary = B'.summary, nodes = [B', C, D, E] }

    data class EffectiveHistory(
        val precedingContext: String,
        val nodes: List<Node>
    )
    fun getEffectiveNodeHistory(chat: Chat, nodeId: String?): EffectiveHistory {
        val historyNodes = mutableListOf<Node>()
        var currentNodeId = nodeId
        var previousSummary = "No previous context exists before this point"

        while (currentNodeId != null) {
            val node = chat.nodes[currentNodeId] ?: break
            historyNodes.add(node)
            node.historySummary?.let {
                previousSummary = it
                break
            }
            currentNodeId = node.parentId
        }
        return EffectiveHistory(
            precedingContext = previousSummary,
            nodes = historyNodes.reversed()
        )
    }

    suspend fun generateChatTitle(systemExpert: Expert, chat: Chat): String {
        chatUtilsMutex.withLock {
            val oldTitle = chat.title
            val maxTextLen = 250
            val sessionPtr = systemExpert.sessionPtr ?: error(
                "Generating chat title for ${chat.title} return early (sysExpert = ${systemExpert.sessionPtr})"
            )
            val effectiveHistory = getEffectiveNodeHistory(chat, chat.cursorNodeId)
            val messages = effectiveHistory.nodes
                .mapNotNull { it.message }
                .map { message ->
                    if (
                        message.author.type == AuthorType.ASSISTANT &&
                        message.text.length > maxTextLen
                    ) {
                        message.copy(text = message.text.take(maxTextLen) + "...")
                    } else {
                        message
                    }
                }
                .toMutableList()

            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.USER),
                text = PROMPTS.TITLE_GENERATION
            )

            val prompt = AppUtils.buildPrompt(systemExpert, messages)
                .replace("{CURRENT_TITLE}", oldTitle)
                .replace("{PREVIOUS_SUMMARY}", effectiveHistory.precedingContext)

            Trace.log("TITLE GEN START session=$sessionPtr")

            val result = StringBuilder()
            LlmPortal.getResponse(sessionPtr, prompt).collect { token ->
                result.append(token)
            }

            Trace.log("TITLE GEN END session=$sessionPtr")

            val newTitle = result.toString().trim()
            return newTitle.ifEmpty { oldTitle }
        }
    }

    suspend fun generateHistorySummary(
        systemExpert: Expert,
        chat: Chat,
        node: Node
    ): String {
        chatUtilsMutex.withLock {
            val sessionPtr = systemExpert.sessionPtr
                ?: error(
                    "Generating history summary for ${chat.title} return early " +
                            "(sysExpert = ${systemExpert.sessionPtr})"
                )

            val effectiveHistory = getEffectiveNodeHistory(chat, node.id)

            val messages = mutableListOf<ChatMessage>()

            // Context summarized before the nodes below.
            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.USER),
                text = "Preceding context:\n${effectiveHistory.precedingContext}"
            )

            // The summary for `node` is up to, but does not include, `node`.
            messages += effectiveHistory.nodes
                .dropLast(1)
                .mapNotNull { it.message }

            // Tell the model what to do after presenting the context/conversation.
            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.USER),
                text = PROMPTS.HISTORY_SUMMARY_GENERATION
            )

            val prompt = AppUtils.buildPrompt(systemExpert, messages)

            Trace.log(
                "MAINT: HISTORY SUMMARY START session=$sessionPtr node=${node.id}"
            )

            val result = StringBuilder()
            try {
                LlmPortal.getResponse(sessionPtr, prompt).collect { token ->
                    result.append(token)
                }
            } catch (e: CancellationException) {
                Trace.log("MAINT: HISTORY SUMMARY ABORTED node=${node.id}")
                throw e
            }

            Trace.log(
                "MAINT: HISTORY SUMMARY END session=$sessionPtr node=${node.id}"
            )

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
