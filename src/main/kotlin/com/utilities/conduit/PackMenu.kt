package com.utilities.conduit

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/* This is responsible for the Packs menu item in the ChatControllerView. It reads all packs found
 * in $APPDIR/packs/...json, and loads them into a PackConfig object to be rendered by the menu below.
 */
@Serializable
data class PackConfig(
    val id: String,
    val name: String = "", // overwritten with filename (minus .json) on loading
    val description: String,
    val experts: List<ExpertConfig>
)

@Serializable
data class ExpertConfig(
    val id: String,
    val type: String,
    val nickname: String,
    val expertise: String,
    val modelPath: String,
    val seedPrompt: String
)

@Composable
fun PackMenu(state: AppState) {
    val appActions = LocalActions.current
    var showMenu by remember { mutableStateOf(false) }
    val currentPack = state.currentPack.value

    // Needed to warn user before actual switching AFTER selection
    var packPendingSwitch by remember { mutableStateOf<PackConfig?>(null) }

    // Show existing pack / Button to bring up Packs menu
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showMenu = true },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Pack: ${currentPack?.name ?: "Select"}")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowDropDown, null)
        }
    }

    // Menu of available Packs in $APPDIR/packs/...json
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Select Pack of Experts") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.loadedPacks ?: emptyList()) { packConfig ->
                        PackCard(packConfig, isSelected = packConfig.id == currentPack?.id) {
                            if (packConfig.id != currentPack?.id) {
                                // Trigger confirmation dialog instead of switching immediately
                                packPendingSwitch = packConfig
                            }
                            showMenu = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMenu = false }) { Text("Close") } }
        )
    }

    // Confirmation Dialog for switching
    packPendingSwitch?.let { pendingPack ->
        AlertDialog(
            onDismissRequest = { packPendingSwitch = null },
            title = { Text("Switch Pack?") },
            text = { Text("Switching to '${pendingPack.name}' may unload current experts. Proceed?") },
            confirmButton = {
                Button(onClick = {
                    appActions.switchPack(pendingPack.name + ".json") // HERE
                    packPendingSwitch = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { packPendingSwitch = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PackCard(packConfig: PackConfig, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pack: ${packConfig.name}", style = MaterialTheme.typography.titleMedium)
            Text(packConfig.description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            packConfig.experts.forEach { expertConfig ->
                Text("• ${expertConfig.nickname} (${expertConfig.type}) - ${expertConfig.expertise}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Loads all .json files in the packs directory and parses them into PackConfig objects.
 */
suspend fun loadAllPackConfigs(): List<PackConfig> = withContext(Dispatchers.IO) {
    val packsDir = java.nio.file.Paths.get(getAppPath(), "packs")

    if (!java.nio.file.Files.exists(packsDir)) return@withContext emptyList()

    java.nio.file.Files.list(packsDir).use { stream ->
        stream.toList()
            .filter { it.toString().endsWith(".json") }
            .mapNotNull { path ->
                try {
                    val jsonContent = java.nio.file.Files.readString(path)
                    val packConfig = AppJson.decodeFromString<PackConfig>(jsonContent)
                    val fileName = path.fileName.toString().removeSuffix(".json")
                    packConfig.copy(name = fileName)
                } catch (e: Exception) {
                    println("Error loading pack ${path.fileName}: ${e.message}")
                    null
                }
            }
    }
}
