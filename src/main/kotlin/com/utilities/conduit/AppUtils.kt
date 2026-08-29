package com.utilities.conduit

import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.ChatMessage
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.ui.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppUtils {

    // Resolves for various OSes - but written by AI and maybe too general - TODO refactor
    fun getAppDir(): String {
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

    fun getNativeLibDir(libName: String): String { return "${getAppDir()}/lib/$libName" }

    fun getPacksDir(): String { return "${getAppDir()}/packs" }

    fun getChatsDir(): String { return "${getAppDir()}/chats" }

    // full pathline to the .gguf file
    fun getAbsoluteModelPath(modelPath: String): String {
        return if (Paths.get(modelPath).isAbsolute)
            modelPath
        else
            Paths.get(getAppDir(), modelPath).toString()
    }

     // Reads all .json files in the packs directory and parses them into Pack objects
     // IMPORTANT: NO PACK EXPERT INITIALIZATIONS (Hence not time-consuming)
    suspend fun getAvailablePacks(): List<Pack> = withContext(Dispatchers.IO) {
        val packsDir = Paths.get(getAppDir(), "packs")

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

    // Returns a prompt wrapped in ChatML - ready for dispatch to the LLM
    // Note currentPrompt is NOT contained in the context. For onSend() the currentPrompt is the
    // last user message. For Maint routines, the current prompt is the system instruction, e.g.
    // Generate title, Summarize chat, etc.
    fun buildChatMlPrompt(
        expertSeedPrompt: String?,
        userModel: String?,
        context: String,
        currentPrompt: String
    ): String {
        return buildString {
            append("<|im_start|>user\n")
            append(expertSeedPrompt?.takeIf { it.isNotBlank() } ?: "You are a general-purpose expert.")
            append("\n<|im_end|>\n")

            append("<|im_start|>user\n")
            append("The current user model is:\n")
            append(userModel?.takeIf { it.isNotBlank() } ?: "No current user model exists.")
            append("\n<|im_end|>\n")

            if (context.isNotBlank()) {
                append("<|im_start|>user\n")
                append("The following is the conversation preceding the current user prompt.")
                append("Each contribution is identified by its author. ")
                append("You may already be one of those contributors.\n\n")
                append(context)
                append("\n<|im_end|>\n")
            }

            append("<|im_start|>user\n")
            append(currentPrompt)
            append("\n<|im_end|>\n")

            append("<|im_start|>assistant\n")
        }
    }

    // Stringifies the list of ChatMessages by prefixing each message with its author and
    // concatenating them.
    // When traversing upwards in Chat history, we stop at a node that has a non-null historySummary
    // That is treated as the "boundaryContext" proxy for all prior chat messages.
    //
    fun getChatContextAsString(
        boundaryContext: String,
        messages: List<ChatMessage>,
        maxAssistantTextLen: Int? = null,
        maxUserTextLen: Int? = null
    ): String {
        val currentMessages = messages.joinToString("\n\n") { message ->
            val role = when (message.author.type) {
                AuthorType.USER -> "USER"
                AuthorType.ASSISTANT -> "ASSISTANT"
                else -> "SYSTEM"
            }

            val text = when {
                message.author.type == AuthorType.ASSISTANT &&
                        maxAssistantTextLen != null &&
                        message.text.length > maxAssistantTextLen ->
                    message.text.take(maxAssistantTextLen) + "..."

                message.author.type == AuthorType.USER &&
                        maxUserTextLen != null &&
                        message.text.length > maxUserTextLen ->
                    message.text.take(maxUserTextLen) + "..."

                else -> message.text
            }

            "$role: $text"
        }

        // Note $context will contain AT LEAST "No previous context is available"
        return "Preceding context: $boundaryContext\n\n$currentMessages"
    }

    // Returns the useful contents of the APPDIR/user-model.json file
    suspend fun getUserModel(): String {
        val appDir = Paths.get(AppUtils.getAppDir())
        val userModelFile = appDir.resolve("user-model.json")

        return withContext(Dispatchers.IO) {
            if (Files.exists(userModelFile)) {
                try {
                    AppJson.decodeFromString<UserModel>(Files.readString(userModelFile)).userModel
                } catch (e: Exception) {
                    Trace.log("USER MODEL: unable to read ${userModelFile.fileName}: ${e.message}")
                    "NO EXISTING USER MODEL"
                }
            } else {
                "NO EXISTING USER MODEL"
            }
        }
    }

    // Used in the ChatsList Menu - TODO: Ought to include time conditionally?
    fun formatDateRange(creation: Long, modification: Long): String {
        val created = Instant.ofEpochMilli(creation).atZone(ZoneId.systemDefault()).toLocalDate()
        val modified = Instant.ofEpochMilli(modification).atZone(ZoneId.systemDefault()).toLocalDate()

        val fmt = DateTimeFormatter.ofPattern("MMM d")

        return if (created == modified) {
            modified.format(fmt)
        } else {
            "${created.format(fmt)} – ${modified.format(fmt)}"
        }
    }
}
