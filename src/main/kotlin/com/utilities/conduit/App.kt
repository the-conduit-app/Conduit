package com.utilities.conduit

import androidx.compose.runtime.*

val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val state = remember { AppState.createNew() }

    LaunchedEffect(Unit) {
        try {
            val defaultPack = Pack.load("Default", state, scope)
            val experts = defaultPack.experts

            val echoExpert = experts.find { it.id == "ECHO.ID" }
            echoExpert?.status = ExpertStatus.READY
            state.setCurrentExpert(echoExpert)
        } catch (e: Exception) {
            println("Init failure: ${e.message}")
        }
    }

    CompositionLocalProvider(LocalActions provides AppActions(scope, state)) {
        MainScreen(state)
    }
}
