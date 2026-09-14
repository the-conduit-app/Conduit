package com.utilities.conduit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.Power
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sun.tools.javac.file.Locations
import com.sun.tools.javac.util.Names
import com.utilities.conduit.ui.MacWindowUtils
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.App
import com.utilities.conduit.ui.ConduitTheme
import com.utilities.conduit.ui.LocalLiquidState
import com.utilities.conduit.ui.splashScreen.SplashScreen
import com.utilities.conduit.utils.AppUtils
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s64
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import java.io.File
import javax.management.Query.and

// global constants
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

fun main() {
    copyAssetsToFilesDir()

    application {
        val scope = rememberCoroutineScope()
        val maxTokensPerResponse = 2048L
        val conduitPtr = remember { LlmPortal.createConduit(maxTokensPerResponse) }
        val liquidState = rememberLiquidState()
        val appState = remember { AppState.createNew(conduitPtr, scope) }
        var showSplashScreen by remember { mutableStateOf(true) }

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

// Fallback items (in case the app dir doesn't already have chats/, packs/, and .approved_models.json
// e.g. on first launch
fun copyAssetsToFilesDir() {
    val clazz = object {}::class.java

    fun copyResource(resourcePath: String, destination: File) {
        if (destination.exists()) return

        try {
            clazz.getResourceAsStream("/assets/$resourcePath")?.use { input ->
                destination.parentFile?.mkdirs()
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: println("Warning: Fallback resource /assets/$resourcePath not found.")
        } catch (ex: Exception) {
            println("Error writing $resourcePath: ${ex.message}")
        }
    }

    // Approved models registry
    copyResource(".approved-models.json", File(AppUtils.getAppDir(), ".approved-models.json"))

    // Empty llm folder with README
    copyResource(
        "llm/README.put-llm-models-here-for-automatic-pickup",
        File(AppUtils.getAppDir(), "llm/README.put-llm-models-here-for-automatic-pickup")
    )

    // Packs — seed individual files only if missing
    listOf("Default.json", "Echoes.json", "Sample.json").forEach { filename ->
        copyResource("packs/$filename", File(AppUtils.getPacksDir(), filename))
    }

    // Chats — seed the entire directory only if it doesn't exist
    val chatsDir = File(AppUtils.getChatsDir())

    if (!chatsDir.exists()) {
        chatsDir.mkdirs()

        val chatFiles = listOf(
//            "A-Tiger-Named-Fangs-9960c9f6-2899-42b3-bcee-2be36fdb4aee.json",
            "Conduit-Conversation-Examples-9398fe88-e799-4f40-9db7-8528b3603e05.json",
            "Conduit-Conversation-Examples-9ad84d36-a601-4379-8584-41dc807e426c.json",
//            "Conduit-Conversation-Examples-c66fb69d-f5e1-43db-b4bb-ee32ec4f3c66.json",
//            "Condy-the-Conduit-and-User-Int-1337fec8-4378-45dd-b807-c7ed3e68c228.json",
            "Getting-help-with-a-monthly-bu-1728c99a-7b15-4b5f-9fdb-59ded5244e3b.json",
//            "Good-evening-greeting-c166802a-5178-4c88-be25-cbe16f9e1f3f.json",
            "Lawyer-Jokes-and-Professional--b0d33652-ba63-4bf8-85e8-4f0571a70180.json",
            "Legal-and-Philosophical-Discus-13ec8d2e-b0c1-4b22-bb17-e750a9c23e76.json",
//            "Modifying-User-Data-4b798ac6-9997-4a3d-b9cf-4f9d374b067d.json",
            "Power-Set-of-Primes-and-Intege-00a56e9c-c96d-4d1f-8917-e7965c590e45.json",
//            "Repurposing-a-Veshti-into-a-Ko-340f9630-3431-414a-9f1a-11a43820d31d.json",
//            "Simple-greets-a-system-ccca615d-ca4c-4853-9251-ce432a9ebd4c.json",
//            "Tiger-Fangs-and-Hanoi-Tomorrow-81098955-18bb-42d7-b9f7-2020ec14a7e6.json",
//            "Tiger-Hare-and-Shark-Conversat-7aa7e391-be85-482c-b31a-d85c8a49c9ef.json",
//            "Tiger-Names-and-Locations-82592dbd-42b1-44e8-b0c0-cb3d1123151d.json",
        )

        chatFiles.forEach { filename ->
            copyResource(
                "chats/$filename",
                File(chatsDir, filename)
            )
        }
    }
}
