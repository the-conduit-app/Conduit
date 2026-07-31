package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.skia.impl.Library.Companion.loaded
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections
import kotlin.io.path.readText
import kotlin.io.path.useLines
import kotlin.io.path.writeText

// --------- IMPORTANT NOTE: The fileName is the unique key in a ChatListItem ---------

data class ChatsListItem(
    val title: String,
    val fileName: String,
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
        val loaded = mutableListOf<ChatsListItem>()

        if (!Files.exists(chatDir)) {
            Files.createDirectories(chatDir)
            return@withContext
        }

        println("Building chats list from $chatDir") ////

        Files.list(chatDir).use { paths ->
            paths.filter { it.fileName.toString().endsWith(".json") }
                .forEach { path ->
                    println("Loading chats list from $path") ////
                    try {
                        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)

                        loaded.add(
                            ChatsListItem(
                                title = extractTitle(path),
                                fileName = path.fileName.toString(),
                                creationTime = attrs.creationTime().toMillis(),
                                modificationTime = attrs.lastModifiedTime().toMillis()
                            )
                        )
                    } catch (e: Exception) {
                        println("Skipping corrupted chat: $path")
                    }
                }
        }
        loaded.sortByDescending { it.modificationTime }

        println("Loaded ${loaded.size} chats") ////

        withContext(Dispatchers.Main) {
            items.clear()
            items.addAll(loaded)
        }
    }

    fun add(item: ChatsListItem) {
        items.removeIf { it.fileName == item.fileName }
        items.add(0, item)
    }

    suspend fun remove(fileName: String): Boolean {
        val index = withContext(Dispatchers.Main) { items.indexOfFirst { it.fileName == fileName } }
        if (index < 0) return false

        val chatPath = Paths.get(getAppPath(), "chats", fileName)

        return withContext(Dispatchers.IO) {
            try {
                Files.deleteIfExists(chatPath)
                withContext(Dispatchers.Main) { items.removeAt(index) }

                // TODO: If this was the current chat, create and load a new chat.

                return@withContext true
            } catch (e: Exception) {
                println("Failed to delete chat: $chatPath")
                return@withContext false
            }
        }
    }

    suspend fun rename(oldItem: ChatsListItem, newItem: ChatsListItem): Boolean {
        val index = withContext(Dispatchers.Main) { items.indexOfFirst { it.fileName == oldItem.fileName } }
        if (index < 0) return false

        val chatDir = Paths.get(getAppPath(), "chats")
        val oldPath = chatDir.resolve(oldItem.fileName)
        val newPath = chatDir.resolve(newItem.fileName)

        return withContext(Dispatchers.IO) {
            try {
                Files.move(oldPath, newPath)
                withContext(Dispatchers.Main) { items[index] = newItem }
                return@withContext true
            } catch (e: Exception) {
                println("Failed to rename chat: $oldPath -> $newPath")
                return@withContext false
            }
        }
    }

    // Update the modification time and move the chat to the top of the list.
    fun touch(item: ChatsListItem) {
        val index = items.indexOfFirst { it.fileName == item.fileName }
        if (index < 0) { // New chat
            items.add(0, item)
            return
        }

        items[index] = item
        Collections.rotate(items.subList(0, index + 1), 1) // move to top

        needsScrollingToTop = true
    }

    // to avoid parsing entire chat.json files, we just read the file for the title field
    // in the early bytes:
    // Assumes simple titles; escaped quotes are a known rebuild-only edge case
    private fun extractTitle(path: Path): String {
        path.useLines { lines ->
            for (line in lines) {
                val pos = line.indexOf("\"title\":")
                if (pos >= 0) {
                    val start = line.indexOf('"', pos + 8) + 1
                    val end = line.indexOf('"', start)
                    if (start > 0 && end > start) {
                        return line.substring(start, end)
                    }
                }
            }
        }
        return "(Untitled)"
    }
}
