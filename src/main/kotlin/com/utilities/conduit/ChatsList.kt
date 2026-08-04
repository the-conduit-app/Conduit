package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.utilities.conduit.AppUtils.getAppPath
import com.utilities.conduit.ChatUtils.makeChatFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections

data class ChatsListItem(
    val chat: Chat,
    val creationTime: Long,
    val modificationTime: Long
)

class ChatsList(
    val items: SnapshotStateList<ChatsListItem> = mutableStateListOf()
) {
    var needsScrollingToTop by mutableStateOf(false)

    // Build from .../chats/*.json chat files.
    suspend fun build() = withContext(Dispatchers.IO) {
        val chatDir = Paths.get(getAppPath(), "chats")
        val chatsById = mutableMapOf<String, ChatsListItem>()

        if (!Files.exists(chatDir)) {
            Files.createDirectories(chatDir)
            return@withContext
        }

        Files.newDirectoryStream(chatDir, "*.json").use { paths ->
            for (path in paths) {
                try {
                    val chat = AppJson.decodeFromString<Chat>(Files.readString(path))
                    chat.restoreTransients()
                    val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)

                    val item = ChatsListItem(
                        chat = chat,
                        creationTime = attrs.creationTime().toMillis(),
                        modificationTime = attrs.lastModifiedTime().toMillis()
                    )

                    val existing = chatsById[chat.id]
                    if (existing == null || item.modificationTime > existing.modificationTime) {
                        if (existing != null) {
                            println("Duplicate chat id ${chat.id}: ignoring older ${path.fileName}")                        }
                        chatsById[chat.id] = item
                    }
                } catch (e: Exception) {
                    println("Skipping corrupted chat: $path")
                }
            }
        }
        val newItems = chatsById.values.toMutableList()
        newItems.sortByDescending { it.modificationTime }
        println("Loaded ${newItems.size} chats")

        withContext(Dispatchers.Main) {
            items.clear()
            items.addAll(newItems)
        }
    }

    fun add(item: ChatsListItem) {
        items.removeIf { it.chat.id == item.chat.id }
        items.add(0, item)
    }

    suspend fun remove(item: ChatsListItem): Boolean {
        val index = withContext(Dispatchers.Main) {
            items.indexOfFirst { it.chat.id == item.chat.id }
        }
        if (index < 0) return false

        val fileName = ChatUtils.makeChatFileName(item.chat.id, item.chat.title)
        val chatDir = Paths.get(getAppPath(), "chats")
        val deletedDir = chatDir.resolve("deleted")

        val source = chatDir.resolve(fileName)
        val target = deletedDir.resolve(fileName)

        return withContext(Dispatchers.IO) {
            try {
                Files.createDirectories(deletedDir)
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)

                withContext(Dispatchers.Main) {
                    items.removeAt(index)
                }

                true
            } catch (e: Exception) {
                println("Failed to delete chat: $source")
                false
            }
        }
    }

    suspend fun rename(item: ChatsListItem, newTitle: String): Chat? {
        val index = withContext(Dispatchers.Main) {
            items.indexOfFirst { it.chat.id == item.chat.id }
        }
        if (index < 0) return null

        return withContext(Dispatchers.IO) {
            val oldTitle = item.chat.title
            try {
                val updatedChat = item.chat.copy(title = newTitle)
                ChatUtils.saveChatToDisk(updatedChat)

                val chatDir = Paths.get(getAppPath(), "chats")
                val oldFileName = makeChatFileName(item.chat.id, oldTitle)
                val oldPath = chatDir.resolve(oldFileName)
                Files.delete(oldPath)

                withContext(Dispatchers.Main) {
                    items[index] = item.copy(chat = updatedChat)
                }
                updatedChat
            } catch (e: Exception) {
                println("Failed chat rename: ${newTitle} ${e.message}")
                item.chat.title = oldTitle // Rollback
                null
            }
        }
    }

    // Update the modification time and move the chat to the top of the list.
    fun touch(item: ChatsListItem) {
        val index = items.indexOfFirst { it.chat.id == item.chat.id }
        if (index < 0) { // New chat
            items.add(0, item)
            return
        }

        items[index] = item
        Collections.rotate(items.subList(0, index + 1), 1) // move to top

        needsScrollingToTop = true
    }
}
