package com.utilities.conduit.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.collections.filter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

// The title is the only property exposed for concealed items (behind front item)
data class DeckItem<T>(
    val title: String,
    val content: T
)

@Composable
fun <T> DeckOfItems(
    deckItems: List<DeckItem<T>>,
    itemSize: DpSize,
    itemOffset: DpOffset = DpOffset(0.dp, 0.dp),
    frontDeckItem: DeckItem<T>?,
    modifier: Modifier = Modifier,
    onFrontItemClick: (DeckItem<T>) -> Unit,
    onFrontItemChanged: (DeckItem<T>) -> Unit,
    itemRenderer: @Composable (item: T, itemSize: DpSize) -> Unit
) {
    var reorderedDeck by remember(deckItems, frontDeckItem) {
        mutableStateOf(deckItems.filter { it != frontDeckItem } + listOfNotNull(frontDeckItem))
    }

    LaunchedEffect(deckItems, frontDeckItem) {
        reorderedDeck = deckItems.filter { it != frontDeckItem } + listOfNotNull(frontDeckItem)
    }

    val deckWidth = itemSize.width + itemOffset.x * (reorderedDeck.size - 1)
    val deckHeight = itemSize.height + itemOffset.y * (reorderedDeck.size - 1)

    // The two cards participating in the current transition.
    var movingFront by remember { mutableStateOf<DeckItem<T>?>(null) }
    var movingSelected by remember { mutableStateOf<DeckItem<T>?>(null) }

    // Horizontal displacement from the card's normal deck position.
    val frontOffset = remember { Animatable(0f) }
    val selectedOffset = remember { Animatable(0f) }

    var animationRunning by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier.size(deckWidth, deckHeight)
        ) {
            reorderedDeck.forEachIndexed { index, item ->

                val isFront = index == reorderedDeck.lastIndex
                val isMovingFront = item == movingFront
                val isMovingSelected = item == movingSelected

                val animatedOffset = when {
                    isMovingFront -> frontOffset.value
                    isMovingSelected -> selectedOffset.value
                    else -> 0f
                }

                Box(
                    modifier = Modifier
                        .size(itemSize)
                        .offset(
                            x = itemOffset.x * index + animatedOffset.dp,
                            y = itemOffset.y * index
                        )
                        .clickable(enabled = !animationRunning) {
                            if (isFront) {
                                onFrontItemClick(item)
                            } else {
                                movingFront = reorderedDeck.last()
                                movingSelected = item
                                animationRunning = true
                            }
                        }
                ) {
                    itemRenderer(item.content, itemSize)
                }
            }
        }
    }

    LaunchedEffect(movingFront, movingSelected) {
        val front = movingFront ?: return@LaunchedEffect
        val selected = movingSelected ?: return@LaunchedEffect

        // Don't start until both values have been established.
        if (!animationRunning) return@LaunchedEffect

        val distance = itemSize.width.value

        // Slide the two cards outward.
        coroutineScope {
            launch {
                frontOffset.animateTo(
                    targetValue = distance,
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            launch {
                selectedOffset.animateTo(
                    targetValue = -distance,
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }

        // Swap the cards while they are off the deck.
        reorderedDeck = reorderedDeck.filter { it != selected && it != front } + front + selected
        onFrontItemChanged(selected)

        // Start the return trip from the same displaced positions.
        coroutineScope {
            launch {
                frontOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            launch {
                selectedOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }

        movingFront = null
        movingSelected = null
        animationRunning = false
    }
}



// Working version below commented out to test animated version above
//// We assume that all cards have the same size.
//// itemSize is the WxH dimensions of the item and offset is by how much the item "in front"
//// is shifted from the one behind it.
//@Composable
//fun<T> DeckOfItems(
//    deckItems: List<DeckItem<T>>,
//    itemSize: DpSize,
//    itemOffset: DpOffset = DpOffset(0.dp, 0.dp),
//    frontDeckItem: DeckItem<T>?,
//    modifier: Modifier = Modifier,
//    onFrontItemClick: (DeckItem<T>) -> Unit,
//    itemRenderer: @Composable (item: T, itemSize: DpSize) -> Unit
//) {
//    var reorderedDeck by remember(deckItems, frontDeckItem) {
//        mutableStateOf(deckItems.filter { it != frontDeckItem } + listOfNotNull(frontDeckItem))
//    }
//    LaunchedEffect(deckItems, frontDeckItem) {
//        reorderedDeck = deckItems.filter { it != frontDeckItem } + listOfNotNull(frontDeckItem)
//    }
//
//    // Lay out the items in the deck assuming each item has the same width and height
//    // as DeckOfItems.modifier.size. Each item is rendered heightOf(item.title) below
//    // the previous one. A maximum of maxItemsToShow is rendered starting from the
//    // frontItem backwards.
//    Box(modifier = modifier) {
//        // Each concealed item exposes just its top (title area). The front item is fully exposed.
//        val deckWidth = itemSize.width + itemOffset.x * (reorderedDeck.size - 1)
//        val deckHeight = itemSize.height + itemOffset.y * (reorderedDeck.size - 1)
//
//        Box(
//            modifier = modifier.size(deckWidth, deckHeight)
//        ) {
//            reorderedDeck.forEachIndexed { index, item ->
//                val isFront = index == reorderedDeck.lastIndex
//                Box(
//                    modifier = Modifier
//                        .size(itemSize)
//                        .offset(
//                            x = itemOffset.x * index,
//                            y = itemOffset.y * index
//                        ).clickable {
//                            if (isFront) {
//                                onFrontItemClick(item)
//                            } else {
//                                reorderedDeck = reorderedDeck.filter { it != item } + item
//                            }
//                        }
//                ) {
//                    itemRenderer(item.content, itemSize)
//                }
//            }
//        }
//    }
//}

// --------------------------------------------------------------------------------------

// Test driver
data class TestItem(
    val title: String,
    val text: String
)

@Composable
fun renderTestItem(
    item: TestItem,
    size: DpSize,
) {
    Surface(
        modifier = Modifier
            .width(size.width)
            .height(size.height),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun DeckOfItemsTest() {
    val testItems = listOf(
        TestItem("One", "ONE"),
        TestItem("Two", "TWO"),
        TestItem("Three", "THREE"),
        TestItem("Four", "FOUR"),
        TestItem("Five", "FIVE")
    )

    val deckItems = testItems.map {
        DeckItem(
            title = it.title,
            content = it
        )
    }

    DeckOfItems(
        deckItems = deckItems,
        itemSize = DpSize(450.dp, 300.dp),
        frontDeckItem = deckItems.last(),
        modifier = Modifier.padding(24.dp),
        onFrontItemClick = { item -> println("Selected: ${item.title}") },
        onFrontItemChanged = { item -> println("Fronted: ${item.title}") },
        itemRenderer = ::renderTestItem
    )
}
