package com.utilities.conduit.ui.chatView

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilities.conduit.ui.InfoBoard
import com.utilities.conduit.ui.SubtleScrollbar

@Composable
fun FullMessagePanel(
    modifier: Modifier,
    text: String,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val maxPanelWidth = maxWidth * 0.65f
        val maxPanelHeight = maxHeight * 0.75f
        val panelShape = RoundedCornerShape(20.dp)

        // Clickout dismisses.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDismiss()
                    }
                }
        )

        InfoBoard(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = maxPanelWidth)
                .heightIn(max = maxPanelHeight)
                .pointerInput(Unit) {
                    detectTapGestures { }
                },
            outerShape = panelShape,
            innerShape = panelShape,
            innerPadding = 20.dp,
            contentPadding = 15.dp,

            content = {
                SelectionContainer {
                    Column(
                        modifier = Modifier.verticalScroll(scrollState)
                    ) {
                        Text(
                            text = text.trim(),
                            fontSize = 14.sp
                        )
                    }
                }
            },

            overlay = {
                SubtleScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                )
            }
        )
    }
}
