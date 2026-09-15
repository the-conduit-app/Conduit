package com.utilities.conduit.utils

import com.utilities.conduit.AppJson
import com.utilities.conduit.packs.Pack
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.ChatMessage
import com.utilities.conduit.debug.Trace
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

    fun getNativeLibDir(libName: String): String {
        val codeSource = File(
            AppUtils::class.java.protectionDomain.codeSource.location.toURI()
        )

        // Packaged .app:
        // Conduit.app/Contents/app/...
        val contentsDir = codeSource.parentFile?.parentFile?.takeIf { it.name == "Contents" }

        if (contentsDir != null) {
            val library = File(contentsDir, "Frameworks/$libName")
            if (library.isFile) {
                return library.absolutePath
            }
        }

        // IDE / Gradle :run:
        // .../Conduit/build/libs/Conduit.jar
        //                  ↑     ↑      ↑
        //                libs   build  Conduit
        val projectDir = codeSource
            .parentFile          // libs
            ?.parentFile         // build
            ?.parentFile         // Conduit
            ?: error("Unable to determine project directory from code source: $codeSource")

        val nativeDir = when (libName) {
            "libconduit.dylib" -> File(projectDir, "cpp/libConduit")
            "libmacwindow.dylib" -> File(projectDir, "cpp/macwindow")
            else -> error("Unknown native library: $libName")
        }

        val library = File(nativeDir, libName)
        if (!library.isFile) {
            error("Native library not found: ${library.absolutePath}")
        }

        return library.absolutePath
    }

    fun getPacksDir(): String { return "${getAppDir()}/packs" }

    fun getChatsDir(): String { return "${getAppDir()}/chats" }

    // Returns the absolute path of the model file when given its filename,
    // searching a bunch of likely places
    fun locateModelFile(filename: String): String? {
        val candidates = listOf(
            File(AppUtils.getAppDir(), "llm/$filename"),
            File(System.getProperty("user.home"), "llm/$filename"),
            File(System.getProperty("user.home"), "Models/$filename"),
            File(System.getProperty("user.home"), "models/$filename"),
            File(System.getProperty("user.home"), "Desktop/$filename")
        )
        return candidates.firstOrNull { file -> file.isFile }?.absolutePath
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
                         Trace.log("Error loading pack ${path.fileName}: ${e.message}")
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
            append(expertSeedPrompt?.takeIf { it.isNotBlank() }
                ?: "You are a general-purpose expert.")
            append("\n<|im_end|>\n")

            append("<|im_start|>user\n")
            append("The current user model is:\n")
            append(userModel?.takeIf { it.isNotBlank() }
                ?: "No current user model exists.")
            append("\n<|im_end|>\n")

            if (context.isNotBlank()) {
                append("<|im_start|>user\n")
                append("The following is historical conversation context provided for background only.\n")
                append("Do not answer or continue any question contained within this context.\n")
                append("Each contribution is identified by its author. ")
                append("You may already be one of those contributors.\n\n")
                append("--- BEGIN HISTORICAL CONVERSATION ---\n")
                append(context)
                append("\n--- END HISTORICAL CONVERSATION ---\n")
                append("<|im_end|>\n")
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
        val maxSystemTextLen = 10   // Effectively omit System responses (from Condy, echo, etc.)

        val currentMessages = messages.joinToString("\n\n") { message ->
            val role = when (message.author.type) {
                AuthorType.USER -> "USER"
                AuthorType.ASSISTANT -> { getContributorLabel(message) ?: "ASSISTANT" }
                else -> "SYSTEM"
            }

            val text = when {
                message.author.type == AuthorType.ASSISTANT &&
                        maxAssistantTextLen != null &&
                        message.text.length > maxAssistantTextLen ->
                    message.text.take(maxAssistantTextLen) + "..."

                message.author.type == AuthorType.SYSTEM &&
                        message.text.length > maxSystemTextLen ->
                    message.text.take(maxSystemTextLen) + "..."

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

    private fun getContributorLabel(message: ChatMessage): String? {
        val title = message.title ?: return null
        return title.substringBefore(" · Pack:").trim().takeIf{ it.isNotEmpty() }
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
