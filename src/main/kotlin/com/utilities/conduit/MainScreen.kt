package com.utilities.conduit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun MainScreen(state: AppState) {
    // launchMaintenance (Aug 5) sweeps thru chats list and renames default titles in the bg
//    LaunchedEffect(Unit) {
//        while (true) {
//            state.chatsList.runMaintenance(state.systemExpert)
//            delay(10.seconds)
//        }
//    }

    Row(modifier = Modifier.fillMaxSize()) {
        AppLeftView(state = state, modifier = Modifier.weight(1f))
        AppRightView(state = state, modifier = Modifier.weight(1f))
    }
}
