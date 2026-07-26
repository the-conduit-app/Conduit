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
}
