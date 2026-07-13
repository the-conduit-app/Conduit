package com.utilities.conduit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpertsPanelView(
    state: AppState,
    onExpertSwitch: (Expert) -> Unit,
    onExpertRetry: (Expert) -> Unit
) {
    val expertList = state.expertsMap.toList()
    if (expertList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            expertList.forEachIndexed { index, expertContainer ->
                val expert: Expert = expertContainer.second

                Box(modifier = Modifier.weight(1f)) {
                    ExpertIcon(state, expert, color = expert.color) {
                        when (expert.status) {
                            ExpertStatus.READY -> onExpertSwitch(expert)
                            ExpertStatus.FAILED -> onExpertRetry(expert)
                            else -> { /* Do nothing while loading */ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpertIcon(state: AppState, expert: Expert?, color: Color, onClick: () -> Unit) {
    val status = expert?.status ?: ExpertStatus.LOADING
    val nickname = expert?.nickname ?: "Expert"
    val backgroundColor = if (status == ExpertStatus.READY) color else Color.LightGray
    val isCurrent = expert != null && state.currentExpert.value?.id == expert.id

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isCurrent) {
            Text("Current Expert", style = TextStyle(fontSize = 10.sp, color = Color.Gray))
        } else {
            Spacer(modifier = Modifier.height(14.dp)) // Maintain alignment
        }
        Surface(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).clickable { onClick() },
            color = backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (status) {
                    ExpertStatus.NONE -> { /* Nothing to do */ }

                    ExpertStatus.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    ExpertStatus.READY -> {
                        IconReady(color)
                        Text(text = nickname, style = TextStyle(fontSize = 14.sp))
                    }
                    ExpertStatus.FAILED -> {
                        IconFailed(color)
                        Text(text = "$nickname Failed to load", style = TextStyle(fontSize = 14.sp))
                    }
                }
            }
        }
    }
}

@Composable
fun IconReady(color: Color) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Expert",
        tint = color,
        modifier = Modifier.size(40.dp)
    )
}

@Composable
fun IconFailed(color: Color) {
    Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = "Expert",
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}
