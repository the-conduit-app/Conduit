package com.utilities.conduit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
    val rightViewOpacity = remember { ViewOpacity() }
    val leftViewOpacity = remember { ViewOpacity() }

    var leftFraction by remember { mutableStateOf(0.35f) }
    var totalWidth by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(
        LocalMessagePanel provides messagePanel,
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

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .pointerInput(totalWidth) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                if (totalWidth > 0) {
                                    val deltaFraction = dragAmount.x / totalWidth

                                    leftFraction = (leftFraction + deltaFraction)
                                        .coerceIn(0.20f, 0.50f)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color.Gray.copy(alpha = 0.35f))
                    )
                }

                AppRightView(
                    state = state,
                    modifier = Modifier.weight(1f - leftFraction)
                )
            }

            MessagePanelView(
                panel = messagePanel,
                maxLines = 3
            )
        }
    }
}
