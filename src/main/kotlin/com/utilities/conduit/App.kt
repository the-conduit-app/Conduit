package com.utilities.conduit

import androidx.compose.runtime.*
import com.sun.jna.Library
import com.sun.jna.Pointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// App level global constants
val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val state = remember { AppState.createNew(scope) }

    LaunchedEffect(Unit) {
        // All Data wrangling in Dispatchers.IO except for updates to observed vars
        withContext(Dispatchers.IO) {
            state.loadedPacks = loadAllPackConfigs()
            state.systemExpert?.initialize(state)

            try {
                val defaultPack = Pack.createAndLoad("Default.json", state, scope)
                val echoExpert = defaultPack.experts.find { it.id == "ECHO.SIMPLE.ID" }

                withContext(Dispatchers.Main) {
                    echoExpert?.status = ExpertStatus.READY
                    state.currentExpert.value = echoExpert
                }
            } catch (e: Exception) {
                println("Init failure: ${e.message}")
            }
        }
    }

    // UI Thread - doesn't need state/data to be finalized before compose
    CompositionLocalProvider(LocalActions provides AppActions(scope, state)) {
        MainScreen(state)
    }
}
