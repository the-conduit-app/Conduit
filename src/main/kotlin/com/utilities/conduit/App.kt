package com.utilities.conduit

import androidx.compose.runtime.*
import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

// App level global constants
val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val state = remember { AppState.createNew(scope) }

    LaunchedEffect(Unit) {
        val packs = withContext(Dispatchers.IO) {
            getAvailablePacks()
        }
        state.availablePacks = packs

        val defaultPack = packs.find { it.id == "Default" } ?: error("Default pack not found")
        defaultPack.initialize(state, scope)

        val echoExpert = defaultPack.experts.find { it.modelPath == InternalExperts.SIMPLE_ECHO }
        state.currentExpert.value = echoExpert
    }

    // UI Thread - doesn't need state/data to be finalized before compose
    CompositionLocalProvider(LocalActions provides AppActions(scope, state)) {
        MainScreen(state)
    }
}

/**
 * Reads all .json files in the packs directory and parses them into Pack objects
 * IMPORTANT: NO PACK EXPERT INITIALIZATIONS
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
