package com.utilities.conduit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue

@Composable
fun ChatHistoryView(state: AppState) {
    val appActions = LocalActions.current
    val pastChatsInfoAsList by  remember(state.pastChatsInfo) {
        derivedStateOf { state.pastChatsInfo.values.toList() }
    }

    LaunchedEffect(Unit) {
        appActions.refreshPastChatsInfo()
    }

    LazyColumn {
        items(
            items = pastChatsInfoAsList,
            key = { it.id }
        ) { chatInfo -> ListItem(
                headlineContent = { Text(text = chatInfo.name) },
                supportingContent = {
                    Text(text = "Modified: ${chatInfo.modificationTime}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.chatManager.loadChatFromDisk(state, chatInfo.id) }
                    .padding(8.dp)
            )
        }
    }
}
