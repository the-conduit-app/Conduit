package com.utilities.conduit

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// The area above the Chat Text Window, with the Packs Menu and Experts
@Composable
fun ChatControlsView(state: AppState) {
    val appActions = LocalActions.current

    Row(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PackMenu(state)
        Spacer(modifier = Modifier.width(16.dp))
        ExpertsPanelView(
            state,
            onExpertSwitch = { newExpert -> appActions.switchExpert(newExpert) },
            onExpertRetry = { expert -> appActions.retryExpertInitialization(expert) }
        )
    }
}
