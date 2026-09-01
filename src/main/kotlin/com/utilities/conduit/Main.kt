package com.utilities.conduit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.utilities.conduit.debug.MacWindowUtils
import com.utilities.conduit.ui.App
import com.utilities.conduit.ui.ConduitTheme
import com.utilities.conduit.utils.AppUtils
import java.awt.EventQueue
import java.io.File

fun main() {
    copyAssetsToFilesDir()

    application {
        Window(
            state = rememberWindowState(
                width = 1280.dp,
                height = 800.dp,
            ),
            title = "Conduit",
            onCloseRequest = ::exitApplication
        ) {
            LaunchedEffect(Unit) {
                MacWindowUtils.hideTitle(window.windowHandle)
            }

            ConduitTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                    //.background(Color(0xFF252A2E))
                ) {
                    App()
                }
            }
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
