package com.utilities.conduit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class LeftPanelMode { HISTORY, TREE }

@Composable
fun LeftPanelContainer(state: AppState, modifier: Modifier = Modifier) {
    val appActions = LocalActions.current

    var currentMode by remember { mutableStateOf(LeftPanelMode.HISTORY) }
    //val savedChats = listOf("Conversation_01", "Legal_Review_v2", "Project_Brainstorm")

    Column(modifier = modifier) {
        when (currentMode) {
            LeftPanelMode.HISTORY -> ChatsListView(state)
            LeftPanelMode.TREE -> TreeView(state)
        }
    }
}
