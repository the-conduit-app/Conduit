package com.utilities.conduit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.Pack

@Composable
fun PackMenu(state: AppState) {
    val appActions = LocalActions.current
    val currentPack = state.currentPack.value
    var showPacksMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { showPacksMenu = true },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pack: ${currentPack?.name ?: "Select"}")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(ConduitIcons.ArrowDropDown, null)
        }
    }

    if (showPacksMenu) {
        AlertDialog(
            onDismissRequest = { showPacksMenu = false },
            title = { Text("Select Pack of Experts") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.availablePacks) { pack ->
                        val isSelected = currentPack == pack

                        PackCard(pack, isSelected) {
                            if (!isSelected) {
                                appActions.switchPack(pack)
                            }
                            showPacksMenu = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPacksMenu = false }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PackCard(pack: Pack, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pack: ${pack.name}", style = MaterialTheme.typography.titleMedium)
            Text(pack.description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            pack.experts.forEach { expertConfig ->
                Text("• ${expertConfig.nickname} (${expertConfig.type}) - ${expertConfig.expertise}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
