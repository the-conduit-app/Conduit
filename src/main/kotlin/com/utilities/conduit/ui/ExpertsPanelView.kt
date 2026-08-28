package com.utilities.conduit.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.Expert

@Composable
fun ExpertsPanelView(
    state: AppState,
    onExpertSwitch: (Expert) -> Unit,
) {
    val expertList = state.currentPack.value?.experts.orEmpty()
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
        expertList.forEachIndexed { index, expert ->
            Box(modifier = Modifier.weight(1f)) {
                val bgColor = if (expert.isReady) ExpertTheme.getExpertColor(index) else Color.LightGray
                ExpertIcon(state, expert, bgColor) {
                    if (expert.isReady)
                        onExpertSwitch(expert)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------------

object ExpertTheme {
    private val colors = listOf(
        Color(0xFF8DB9CC), // Dusty Blue
        Color(0xFFD09AAA), // Dusty Rose
        Color(0xFFD5BF72), // Muted Gold
        Color(0xFFD5A084), // Muted Peach
        Color(0xFF91B99A), // Sage
        Color(0xFF9EADB3)  // Slate
    )

    fun getExpertColor(n: Int): Color =
        colors[n % colors.size]
}

// --------------------------------------------------------------------------------------------

@Composable
fun ExpertIcon(
    state: AppState,
    expert: Expert,
    color: Color,
    onClick: () -> Unit
) {
    val isCurrent = state.currentExpert.value?.id == expert.id

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && expert.isReady) 0.92f else 1f,
        animationSpec = spring(),
        label = "expertScale"
    )

    val iconColor = if (expert.isReady) color else Color.LightGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = expert.isReady,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            expert.expertise,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF007C91),
            maxLines = 1
        )

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (expert.isReady) {
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 48.dp else 40.dp)
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = if (isCurrent)
                                Color(0xFF007C91)
                            else
                                Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(
                            if (isCurrent) 44.dp else 36.dp
                        )
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF007C91),
                    strokeWidth = 2.dp
                )
            }
        }

        Text(
            expert.nickname,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF007C91),
            maxLines = 1
        )
    }
}
