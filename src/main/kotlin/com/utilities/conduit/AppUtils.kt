package com.utilities.conduit

import java.io.File

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

    fun createChatFileName(chatTitle: String, chatId: String): String {
        val sanitizedTitle = chatTitle
            .take(30)
            .replace(Regex("[^a-zA-Z0-9]"), "-")

        return "${sanitizedTitle}-${chatId}.json"
    }

    fun extractChatIdFromFileName(fileName: String): String {
        val match = Regex(
            "-([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\\.json$"
        ).find(fileName)

        return match?.groupValues?.get(1)
            ?: error("Cannot extract chat ID from filename: $fileName")
    }
}
