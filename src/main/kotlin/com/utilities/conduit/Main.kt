package com.utilities.conduit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.system.exitProcess

fun main() {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            LlmExpert.shutdown()
        }
    )

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
