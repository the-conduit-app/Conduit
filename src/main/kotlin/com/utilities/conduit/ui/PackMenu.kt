package com.utilities.conduit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.utilities.conduit.AppState
import com.utilities.conduit.expertColors
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import conduit.generated.resources.plasma_s64
import org.jetbrains.compose.resources.painterResource

@Composable
fun PackMenu(state: AppState) {
    val appActions = LocalActions.current
    val currentPack = state.currentPack.value
    var showPacksMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { showPacksMenu = true },
        color = Color.Transparent
    ) {
        Box {
            Image(
                painter = painterResource(Res.drawable.plasma_s1),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pack: ${currentPack?.name ?: "Select"}")

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = ConduitIcons.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF007C91),
                )
            }
        }
    }

    if (showPacksMenu) {
        BoxWithConstraints {
            val maxMenuHeight = maxHeight * 0.9f
            Dialog(
                onDismissRequest = { showPacksMenu = false }
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(min = 420.dp, max = 600.dp)
                        .heightIn(max = maxMenuHeight)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color.Transparent
                ) {
                    Box {
                        Image(
                            painter = painterResource(Res.drawable.plasma_s64),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.75f
                        )

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Select Expert Pack",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(Modifier.height(12.dp))

                            PackCardList(
                                items = state.availablePacks
                            ) { pack ->
                                val isSelected = currentPack == pack

                                PackCard(
                                    pack = pack,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (!isSelected) {
                                            appActions.switchPack(pack)
                                        }
                                        showPacksMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
