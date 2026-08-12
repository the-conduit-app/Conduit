package com.utilities.conduit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// App level global constants
val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

@Composable
fun App(exitApplication: () -> Unit) {
    val scope = rememberCoroutineScope()

    val maxTokensPerResponse = 2048L
    val conduitPtr = remember { LlmPortal.createConduit(maxTokensPerResponse) }
    val state = remember { AppState.createNew(conduitPtr, scope = scope) }

    LaunchedEffect(Unit) {
        state.chatsList.build()        // load existing chats in the chats dir

        val packs = withContext(Dispatchers.IO) {
            AppUtils.getAvailablePacks()
        }
        state.availablePacks = packs

        val defaultPack = packs.find { it.id == "Default" } ?: error("Default pack not found")
        defaultPack.select(state)
        defaultPack.initializeExperts(state, scope) // may launch several model.inits

        // Launch the system expert
        state.systemExpert.modelPath?.let { modelPath ->
            val absoluteModelPath = AppUtils.getAbsoluteModelPath(modelPath)
            scope.launch(Dispatchers.IO) {
                try {
                    state.systemExpert.sessionPtr = LlmPortal.initialize(state.conduitPtr, absoluteModelPath)
                } catch (e: Exception) {
                    Trace.log("System expert initialization failed: ${e.message}")
                }
            }
        }

        val echoExpert = defaultPack.experts.find { it.modelPath == InternalExperts.SIMPLE_ECHO }
        state.currentExpert.value = echoExpert
    }
    DisposableEffect(Unit) {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runBlocking {
                    state.shutdown()
                }
            }
        )
        onDispose { }
    }

    // -----------------------------------------------------------------------------------------

    // Monitoring user activity for maint jobs (renaming chats, etc.)
    LaunchedEffect(Unit) {
        UserActivityMonitor.start(scope, onIdleJob = { Maintenance.start(state, scope) })
    }
    DisposableEffect(Unit) {
        onDispose {
            UserActivityMonitor.stop()
        }
    }

    //------------------------------------------------------------------------------------------

    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 800.dp
    )

    Window(
        state = windowState,
        title = "Conduit Workspace",
        onCloseRequest = { exitApplication() }
    ) {
        CompositionLocalProvider(LocalActions provides AppActions(scope, state)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .userActivityMonitor()
            ) {
                MainScreen(state)
            }
        }
    }
}
