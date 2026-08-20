package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.sun.jna.Pointer
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.ChatManager
import com.utilities.conduit.chat.ChatMessage
import com.utilities.conduit.chat.ChatsList
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.LeftPanelMode
import com.utilities.conduit.ui.Notification
import com.utilities.conduit.ui.ScreenCurtain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
