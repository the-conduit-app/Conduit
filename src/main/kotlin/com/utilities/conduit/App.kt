package com.utilities.conduit

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

// App level global constants
val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

@Composable
fun App(exitApplication: () -> Unit) {
    val scope = rememberCoroutineScope()

    val maxTokensPerResponse = 2048L
    val conduitPtr = remember { LlmPortal.createConduit(maxTokensPerResponse) }
    val state = remember { AppState.createNew(conduitPtr, scope = scope) }

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

    LaunchedEffect(Unit) {
        state.chatsList.build()        // load existing chats in the chats dir

        val packs = withContext(Dispatchers.IO) {
            getAvailablePacks()
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
    LaunchedEffect(Unit) { // Low priority maintenance loop (cleanup chats etc.)
        Maintenance.start(state, scope)
    }

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
            MainScreen(state)
        }
    }
}

/**
 * Reads all .json files in the packs directory and parses them into Pack objects
 * IMPORTANT: NO PACK EXPERT INITIALIZATIONS (Hence not time-consuming)
 */
suspend fun getAvailablePacks(): List<Pack> = withContext(Dispatchers.IO) {
    val packsDir = Paths.get(getAppPath(), "packs")

    if (!Files.exists(packsDir))
        return@withContext emptyList()

    Files.list(packsDir).use { stream ->
        stream.toList()
            .sortedBy { it.fileName.toString() }
            .filter { it.toString().endsWith(".json") }
            .mapNotNull { path ->
                try {
                    val id = path.fileName.toString().removeSuffix(".json")
                    val json = Files.readString(path)
                    AppJson.decodeFromString<Pack>(json).copy(id = id)
                } catch (e: Exception) {
                    println("Error loading pack ${path.fileName}: ${e.message}")
                    null
                }
            }
    }
}
