package com.utilities.conduit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BranchingPopup(
    state: AppState,
    node: Node,
    onDismiss: () -> Unit,
    onSelectBranch: (Node) -> Unit
) {
    val scope = rememberCoroutineScope()

    val currentChat = state.chatManager.currentChat
    val candidateBranches = node.children
        .mapNotNull { childId -> currentChat.nodes[childId] }
        .filterNot { child -> ChatUtils.leadsToCursor(currentChat, child) }

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium, tonalElevation = 6.dp, shadowElevation = 8.dp,
            modifier = Modifier.widthIn(min = 220.dp, max = 420.dp)
        ) {
            val onBranchFromHere: () -> Unit =
                if (candidateBranches.isEmpty()) {
                    {
                        scope.launch {
                            state.chatManager.setCursor(node)
                            onDismiss()
                        }
                    }
                } else {
                    {
                        onSelectBranch(node)
                    }
                }

            Column {
                Text(
                    text = "Start new branch from here",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBranchFromHere)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (candidateBranches.isNotEmpty()) {
                    HorizontalDivider()

                    Text(
                        text = "Or select existing branch",
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(candidateBranches) { branch ->

                        val message = branch.message
                            val title = message?.title ?: "Unknown"
                            val date = message?.timestamp?.let {
                                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it))
                            } ?: ""
                            val text = message?.text?.trim() ?: "Empty"

                            Text(
                                text = "$title · $date: $text",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectBranch(branch)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
