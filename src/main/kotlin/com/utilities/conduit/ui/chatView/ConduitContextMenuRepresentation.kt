package com.utilities.conduit.ui.chatView

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberCursorPositionProvider
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s64
import org.jetbrains.compose.resources.painterResource

val ConduitContextMenuRepresentation = object : ContextMenuRepresentation {
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
                        .widthIn(min = 180.dp, max = 600.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = painterResource(Res.drawable.plasma_s64),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    val menuItems = items()
                    Column(modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = 180.dp)
                    ) {
                        menuItems.forEach { item ->
                            val isCurrent = item.label.startsWith("✓")
                            val label = if (isCurrent) item.label.removePrefix("✓") else item.label
                            val startNewBranch = item.label.startsWith("Start new")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        item.onClick()
                                        state.status = ContextMenuState.Status.Closed
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(modifier = Modifier.width(20.dp)) {
                                    when {
                                        startNewBranch -> Text("⎇")
                                        isCurrent -> Text("✓")
                                    }
                                }
                                Text(text = label.trimStart(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.25f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 2f
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
