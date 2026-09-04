package com.utilities.conduit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.utilities.conduit.debug.MacWindowUtils
import com.utilities.conduit.ui.App
import com.utilities.conduit.ui.ConduitTheme
import com.utilities.conduit.ui.splashScreen.SplashScreen
import com.utilities.conduit.utils.AppUtils
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import conduit.generated.resources.plasma_s64
import org.jetbrains.compose.resources.painterResource
import java.awt.EventQueue
import java.io.File

fun main() {
    copyAssetsToFilesDir()

    application {
        var showSplashScreen by remember { mutableStateOf(true) }

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
                MacWindowUtils.styleTitlebar(window.windowHandle)
            }

            val titleBarHeight = 28.dp
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(Res.drawable.plasma_s64),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.5f
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(titleBarHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Conduit",
                        style = ConduitTheme.Typography.titleMedium.copy(
                            color = Color(0xFF006477)
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = titleBarHeight)
                ) {
                    App()
                }
                // Splash overlay

                if (showSplashScreen) {
                    SplashScreen(
                        onDismiss = { showSplashScreen = false }
                    )
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
                    //println("Copying $filename") ////
                    input.copyTo(output)
                }
            } ?: println("Warning: Resource /assets/packs/$filename not found.")
        } catch (ex: Exception) {
            println("Error writing $filename: ${ex.message}")
        }
    }
}
