package com.utilities.conduit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import kotlin.math.roundToInt
@Composable
fun MainScreen(state: AppState) {
    val scope = rememberCoroutineScope()

    val messagePanel = remember {
        MessagePanel(scope)
    }

    CompositionLocalProvider(
        LocalMessagePanel provides messagePanel
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                AppLeftView(
                    state = state,
                    modifier = Modifier.weight(1f)
                )
                AppRightView(
                    state = state,
                    modifier = Modifier.weight(1f)
                )
            }

            MessagePanelView(
                panel = messagePanel,
                maxLines = 3
            )
        }
    }
}
