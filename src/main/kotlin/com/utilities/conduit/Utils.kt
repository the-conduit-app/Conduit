package com.utilities.conduit

import java.io.File

object GeneralUtils {
    fun getAppDir(): File {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")

        val appDir = when {
            os.contains("mac") -> File(home, "Library/Application Support/Conduit")
            os.contains("win") -> File(System.getenv("APPDATA"), "Conduit")
            else -> File(home, ".config/conduit")
        }

        if (!appDir.exists()) appDir.mkdirs()
        return appDir
    }
}
