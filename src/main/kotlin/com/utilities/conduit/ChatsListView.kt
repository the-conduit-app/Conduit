package com.utilities.conduit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant

@Composable
fun ChatsListView(state: AppState) {
    val chatsList = state.chatsList

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "\uD83C\uDF33",      //// temporary tree glyph
                modifier = Modifier.clickable {
                    // TODO Toggle to ChatTreeView
                }
            )
        }

        HorizontalDivider()

        // --------------------------------------------------------------------
        // Chats
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
        ) {
            items(
                items = chatsList.items,
                key = { it.fileName }
            ) { item ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            state.chatManager.loadChatFromDisk(state, item.fileName)
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "(${formatDateRange(item.creationTime, item.modificationTime)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatDateRange(
    creation: Long,
    modification: Long
): String {

    val created = Instant.ofEpochMilli(creation).atZone(ZoneId.systemDefault()).toLocalDate()
    val modified = Instant.ofEpochMilli(modification).atZone(ZoneId.systemDefault()).toLocalDate()

    val fmt = DateTimeFormatter.ofPattern("MMM d")

    return if (created == modified) {
        modified.format(fmt)
    } else {
        "${created.format(fmt)} – ${modified.format(fmt)}"
    }
}
