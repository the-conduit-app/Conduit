package com.utilities.conduit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import kotlin.system.exitProcess

fun main() {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            LlmExpert.shutdown()
        }
    )

    // Create fresh copies of packs/{Default,Echoes,Sample}.json
    copyAssetsToFilesDir()

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

        Window(
            onCloseRequest = {
                println("Exiting...")
                LlmExpert.shutdown()
                exitProcess(0)
            },
            state = windowState,
            title = "Conduit Workspace"
        ) {
            App()
        }
    }
}

fun copyAssetsToFilesDir() {
    val packsDir = File(AppUtils.getPacksDir()).apply { mkdirs() }

    listOf("Default.json", "Echoes.json", "Sample.json").forEach { filename ->
        val outFile = File(packsDir, filename)
        try {
            object {}::class.java.getResourceAsStream("/assets/packs/$filename")?.use { input ->
                outFile.outputStream().use { output ->
                    println("Copying $filename") ////
                    input.copyTo(output)
                }
            } ?: println("Warning: Resource /assets/packs/$filename not found.")
        } catch (ex: Exception) {
            println("Error writing $filename: ${ex.message}")
        }
    }
}
