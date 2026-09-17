package com.utilities.conduit.packs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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

// --------------------------------------------------------------------------------------

//// Test driver
//data class TestItem(
//    val title: String,
//    val text: String
//)
//
//@Composable
//fun renderTestItem(
//    item: TestItem,
//    size: DpSize,
//) {
//    Surface(
//        modifier = Modifier
//            .width(size.width)
//            .height(size.height),
//        shape = MaterialTheme.shapes.medium,
//        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
//        border = BorderStroke(
//            1.dp,
//            MaterialTheme.colorScheme.outline
//        )
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            Text(
//                text = item.title,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(40.dp)
//                    .padding(horizontal = 12.dp),
//                style = MaterialTheme.typography.titleMedium
//            )
//
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = item.text,
//                    style = MaterialTheme.typography.headlineMedium
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun DeckOfItemsTest() {
//    val testItems = listOf(
//        TestItem("One", "ONE"),
//        TestItem("Two", "TWO"),
//        TestItem("Three", "THREE"),
//        TestItem("Four", "FOUR"),
//        TestItem("Five", "FIVE")
//    )
//
//    val deckItems = testItems.map {
//        DeckItem(
//            title = it.title,
//            content = it
//        )
//    }
//
//    DeckOfItems(
//        deckItems = deckItems,
//        itemSize = DpSize(450.dp, 300.dp),
//        frontDeckItem = deckItems.last(),
//        modifier = Modifier.padding(24.dp),
//        onFrontItemClick = { item -> println("Selected: ${item.title}") },
//        onFrontItemChanged = { item -> println("Fronted: ${item.title}") },
//        itemRenderer = ::renderTestItem
//    )
//}
