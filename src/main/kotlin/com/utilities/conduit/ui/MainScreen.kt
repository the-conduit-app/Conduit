package com.utilities.conduit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.utilities.conduit.AppState

@Composable
fun MainScreen(state: AppState) {
    val scope = rememberCoroutineScope()

    val messagePanelController = remember {
        MessagePanelController(scope)
    }
    val rightViewOpacity = remember { ViewOpacity() }
    val leftViewOpacity = remember { ViewOpacity() }

    var leftFraction by remember { mutableStateOf(0.35f) }
    var totalWidth by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(
        LocalMessagePanelController provides messagePanelController,
        LocalRightViewOpacity provides rightViewOpacity,
        LocalLeftViewOpacity provides leftViewOpacity
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { totalWidth = it.width }
            ) {
                AppLeftView(
                    state = state,
                    modifier = Modifier.weight(leftFraction)
                )

                VerticalDivider(
                    totalWidth = totalWidth,
                    leftFraction = leftFraction,
                    onLeftFractionChange = { leftFraction = it }
                )

                AppRightView(
                    state = state,
                    modifier = Modifier.weight(1f - leftFraction)
                )
            }

            MessagePanel(
                panel = messagePanelController,
                maxLines = 3
            )
        }
    }
}
