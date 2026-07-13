package com.utilities.conduit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PackView(state: AppState) {
    val appActions = LocalActions.current
    var expanded by remember { mutableStateOf(false) }
    val availablePacks = remember { getAvailablePacks() }
    var menuSelection by remember { mutableStateOf<String?>(null) }

    val currentPack = state.currentPack.value
    val currentPackName = currentPack?.name ?: "No Pack Yet"

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Surface(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { expanded = true }.padding(8.dp),
            color = Color.LightGray
        ) {
            Text(text = "Pack: $currentPackName ▼")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availablePacks.forEach { packName ->
                DropdownMenuItem(
                    text = { Text(packName) },
                    onClick = {
                        menuSelection = packName
                        expanded = false
                    }
                )
            }
        }
    }

    if (menuSelection != null && menuSelection != currentPackName) {
        AlertDialog(
            onDismissRequest = { menuSelection = null },
            title = { Text("Switch Pack?") },
            text = { Text("Switching to '$menuSelection' may unload current experts. Proceed?") },
            confirmButton = {
                Button(onClick = {
                    menuSelection?.let { appActions.switchPack(it) }
                    menuSelection = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { menuSelection = null }) { Text("Cancel") }
            }
        )
    }
}

private fun getAvailablePacks(): List<String> {
    val packsDir = File(GeneralUtils.getAppDir(), "packs")
    return packsDir.listFiles { _, name -> name.endsWith(".json") }?.map {
        it.name.removeSuffix(".json")
    } ?: emptyList()
}
