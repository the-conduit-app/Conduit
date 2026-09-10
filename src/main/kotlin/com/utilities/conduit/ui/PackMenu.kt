package com.utilities.conduit.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.utilities.conduit.AppState
import com.utilities.conduit.Expert
import com.utilities.conduit.Pack
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import conduit.generated.resources.plasma_s64
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import org.jetbrains.compose.resources.painterResource

@Composable
fun PackMenu(state: AppState) {
    val appActions = LocalActions.current
    val currentPack = state.currentPack.value
    var showPacksMenu by remember { mutableStateOf(false) }

    Box {
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
                    alpha = 0.75f
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
                                alpha = 0.5f
                            )

                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Select Expert Pack",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(Modifier.height(12.dp))

                                val deckItems = state.availablePacks.map { pack ->
                                    DeckItem(
                                        title = "${pack.name} - ${pack.description}",
                                        content = pack
                                    )
                                }

                                DeckOfItems(
                                    deckItems = deckItems,
                                    itemSize = DpSize(400.dp, 250.dp),
                                    itemOffset = DpOffset(4.dp, 40.dp),
                                    frontDeckItem = deckItems.find {
                                        it.content.id == currentPack?.id
                                    },
                                    onFrontItemClick = { item ->
                                        appActions.switchPack(item.content)
                                        showPacksMenu = false
                                    },
                                    onFrontItemChanged = { item ->
                                        ////Sounds.SwishSwash.play() // TODO
                                        appActions.switchPack(item.content)
                                    },
                                    itemRenderer = ::renderPackInfo
                                )

                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun renderPackInfo(
    pack: Pack?, //// TODO make non-nullable
    size: DpSize,
) {
    if (pack == null) return //// TODO see above
    var hoveredExpert by remember { mutableStateOf<Expert?>(null) }

    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            modifier = Modifier.size(size)
        ) {
            Image(
                painter = painterResource(Res.drawable.plasma_s1),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(LocalLiquidState.current)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .liquid(LocalLiquidState.current) {
                        frost = 10.dp
                        shape = RoundedCornerShape(10.dp)
                        refraction = 0.25f
                        curve = 0.25f
                        edge = 1f
                        tint = Color.Red.copy(alpha = 0.1f)
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = "${pack.name} - ${pack.description}",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                pack.experts.forEach { expert ->
                    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .onPointerEvent(PointerEventType.Enter) { event -> hoveredExpert = expert }
                            .onPointerEvent(PointerEventType.Exit) { hoveredExpert = null },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            tint = CONDUIT_COLORS[expert.color] ?: Color.Gray,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${expert.nickname} (${expert.expertise}) - ${expert.description}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val expertDetailsAlpha by animateFloatAsState(
                    targetValue = if (hoveredExpert != null) 1f else 0f,
                    animationSpec = tween(250),
                    label = "expertDetailAlpha"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(expertDetailsAlpha)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = hoveredExpert?.description ?: "",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = hoveredExpert?.model ?: "",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
