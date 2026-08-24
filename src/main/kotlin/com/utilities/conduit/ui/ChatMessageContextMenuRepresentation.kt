package com.utilities.conduit.ui

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberCursorPositionProvider

val ChatMessageContextMenuRepresentation = object : ContextMenuRepresentation
{
    @Composable
    override fun Representation(
        state: ContextMenuState,
        items: () -> List<ContextMenuItem>
    ) {
        if (state.status is ContextMenuState.Status.Open) {
            Popup(
                popupPositionProvider = rememberCursorPositionProvider(
                    alignment = Alignment.BottomEnd,
                ),
                onDismissRequest = {
                    state.status = ContextMenuState.Status.Closed
                },
                properties = PopupProperties(
                    focusable = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(
                            min = 180.dp,
                            max = 600.dp
                        )
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    val menuItems = items()
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        menuItems.forEachIndexed { index, item ->
                            val isCurrent = item.label.startsWith("✓")
                            val label = if (isCurrent) item.label.removePrefix("✓") else item.label
                            val isNewBranch = item.label.startsWith("Start new")

                            Row(
                                modifier = Modifier
                                    .clickable {
                                        item.onClick()
                                        state.status = ContextMenuState.Status.Closed
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(modifier = Modifier.width(20.dp)) {
                                    when {
                                        isNewBranch -> Text("⎇")
                                        isCurrent -> Text("✓")
                                    }
                                }
                                Text(label.trimStart())
                            }
                        }
                    }
                }
            }
        }
    }
}
