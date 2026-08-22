package com.utilities.conduit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.utilities.conduit.*
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// App level global constants
val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

@Composable
fun App() {
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
        defaultPack.initializeExperts(state) // may launch several model.inits

        // Launch the system expert
        state.systemExpert.modelPath?.let { modelPath ->
            val absoluteModelPath = AppUtils.getAbsoluteModelPath(modelPath)
            withContext(Dispatchers.IO) {
                try {
                    state.systemExpert.sessionPtr = LlmPortal.initialize(state.conduitPtr, absoluteModelPath)
                } catch (e: Exception) {
                    Trace.log("System expert initialization failed: ${e.message}")
                }
            }
        }

        val echoExpert = defaultPack.experts.find { it.modelPath == InternalExperts.SIMPLE_ECHO }
        state.currentExpert.value = echoExpert
        Maintenance.start(state, scope)
    }
    DisposableEffect(Unit) {
        val hook = Thread {
            scope.launch { state.shutdown() }
        }
        Runtime.getRuntime().addShutdownHook(hook)

        onDispose {
            Runtime.getRuntime().removeShutdownHook(hook)
        }
    }

    //------------------------------------------------------------------------------------------

    val fullMessageOverlayState = remember { FullMessageOverlayState() }

    CompositionLocalProvider(
        LocalActions provides AppActions(state, fullMessageOverlayState)
    ) {
        val appActions = LocalActions.current

        Box(
            Modifier
                .fillMaxSize()
                .userActivityMonitor(state)
        ) {
            MainScreen(state)

            appActions.fullMessageText?.let { text ->
                FullMessagePanel(
                    modifier = Modifier.zIndex(1f),
                    text = text,
                    onDismiss = {
                        appActions.dismissFullMessage()
                    }
                )
            }
        }
    }
}
