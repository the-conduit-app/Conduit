package com.utilities.conduit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(state: AppState) {
    Row(modifier = Modifier.fillMaxSize()) {
        AppLeftView(state = state, modifier = Modifier.weight(1f))
        AppRightView(state = state, modifier = Modifier.weight(1f))
    }
}
