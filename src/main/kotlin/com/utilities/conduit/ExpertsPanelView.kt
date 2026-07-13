package com.utilities.conduit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExpertsPanelView(
    state: AppState,
    onExpertSwitch: (Expert) -> Unit,
    onExpertRetry: (Expert) -> Unit
) {
    val expertList = state.expertsMap.values.toList()
    if (expertList.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
            Text("There are no experts in this pack.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        expertList.forEach { expert ->
            Box(modifier = Modifier.weight(1f)) {
                ExpertIcon(state, expert) {
                    when (expert.status) {
                        ExpertStatus.READY -> onExpertSwitch(expert)
                        ExpertStatus.FAILED -> onExpertRetry(expert)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ExpertIcon(state: AppState, expert: Expert, onClick: () -> Unit) {
    val isCurrent = state.currentExpert.value?.id == expert.id
    val backgroundColor = if (expert.status == ExpertStatus.READY) expert.color else Color.LightGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.width(60.dp).height(60.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                when (expert.status) {
                    ExpertStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    ExpertStatus.READY -> Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    ExpertStatus.FAILED -> Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    else -> {}
                }
                Text(expert.nickname, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
            }
        }
        Text(expert.expertise, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
    }
}
