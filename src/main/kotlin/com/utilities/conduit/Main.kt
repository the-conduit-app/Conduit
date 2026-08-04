package com.utilities.conduit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

fun main() {
    copyAssetsToFilesDir()

    application {
        Trace.log("Main launching app") ////
        App(exitApplication = ::exitApplication,
        onExit = {
            Trace.log("Exit hook entered")
            runBlocking {
                Trace.log("Shutdown coroutine entered")
                state.shutdown()
                Trace.log("Shutdown complete")
            }
            Trace.log("Exit hook returning")
        })
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
