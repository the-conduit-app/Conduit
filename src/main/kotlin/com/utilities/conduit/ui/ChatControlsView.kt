package com.utilities.conduit.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import io.github.fletchmckee.liquid.liquid

// The area above the Chat Text Window, with the Packs Menu and Experts
@Composable
fun ChatControlsView(state: AppState) {
    val appActions = LocalActions.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .liquid(LocalLiquidState.current) {
                    frost = 2.dp
                    shape = RoundedCornerShape(10.dp)
                    refraction = 0.75f
                    curve = .75f
                    edge = 1f
                    tint = Color.Red.copy(alpha = 0.05f)
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PackMenu(state)
            Spacer(modifier = Modifier.width(16.dp))
            ExpertsPanelView(
                state,
                onExpertSwitch = { newExpert -> appActions.switchExpert(newExpert) }
            )
        }
    }
}
