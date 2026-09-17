package com.utilities.conduit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.App
import com.utilities.conduit.ui.ConduitTheme
import com.utilities.conduit.ui.LocalLiquidState
import com.utilities.conduit.ui.MacWindowUtils
import com.utilities.conduit.ui.splashScreen.SplashScreen
import com.utilities.conduit.utils.AppUtils
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s64
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

// global constants
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

fun main() {
    var seedChatIds: List<String> = listOf()

    try {
        seedChatIds = AppUtils.copyAssetsToFilesDir() // starter packs, chats and approved-models.json
    } catch (e: Exception) {
        System.err.println("ERROR: ${e.message}")
        ConduitLog.error("Startup failure", e)
        System.exit(1)
    }

    application {
        val scope = rememberCoroutineScope()
        val maxTokensPerResponse = 2048L // Contain runaway models like Smol
        val conduitPtr = remember { LlmPortal.createConduit(maxTokensPerResponse) }
        val liquidState = rememberLiquidState()
        var showSplashScreen by remember { mutableStateOf(true) }

        val appState = remember {
            AppState.createNew(conduitPtr, scope).also { state ->
                seedChatIds.forEach { state.addToSeedChats(it) }
            }
        }

        CompositionLocalProvider(LocalLiquidState provides liquidState) {
            Window(
                state = rememberWindowState(
                    width = 1200.dp,
                    height = 800.dp,
                ),
                title = "Conduit",
                onCloseRequest = ::exitApplication,
                resizable = true
            ) {
                val windowHandle = window.windowHandle

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main application is always underneath the splash.
                    MainContent(
                        state = appState,
                        windowHandle = windowHandle
                    )

                    // Splash sits over the main content during startup.
                    if (showSplashScreen) {
                        SplashScreen(
                            appState = appState,
                            onReady = {
                                showSplashScreen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    state: AppState,
    windowHandle: Long
) {
    LaunchedEffect(Unit) {
        MacWindowUtils.hideTitle(windowHandle)
        MacWindowUtils.styleTitlebar(windowHandle)
        MacWindowUtils.setMagnificationCallback(windowHandle) // for TreeView pinch-zoom
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
            ConduitTheme {
                App(state)
            }
        }
    }
}
