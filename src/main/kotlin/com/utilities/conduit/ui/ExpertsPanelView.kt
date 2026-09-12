package com.utilities.conduit.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.Expert
import com.utilities.conduit.ExpertStatus
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import conduit.generated.resources.plasma_s64
import org.jetbrains.compose.resources.painterResource

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
                ExpertIcon(state, expert) {
                    if (expert.isReady)
                        onExpertSwitch(expert)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------------

@Composable
fun ExpertIcon(
    state: AppState,
    expert: Expert,
    onClick: () -> Unit
) {
    val isCurrent = state.currentExpert.value?.id == expert.id

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && expert.isReady) 0.75f else 1f,
        animationSpec = spring(),
        label = "expertScale"
    )

    val expertColor = CONDUIT_COLORS[expert.color] ?: Color.Gray
    val iconColor = if (expert.isReady) expertColor else Color.LightGray
    val iconBoxSize = 52.dp
    val density = LocalDensity.current

    val textShadow = with(density) {
        Shadow(
            color = Color.White.copy(alpha = 0.9f),
            offset = Offset(0f, 2.dp.toPx()),
            blurRadius = 7.dp.toPx()
        )
    }

    val iconSize = iconBoxSize * if (isCurrent) 0.9f else 0.65f
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
            text = expert.expertise,
            style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
            color = Color(0xFF007C91),
            maxLines = 1,
        )

        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                CurrentExpertDecoration(
                    color = expertColor,
                    size = iconBoxSize
                )
            }

            // The Person Icon
            when (expert.status) {
                ExpertStatus.READY -> {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = expertColor,
                        modifier = Modifier.size(
                            iconBoxSize * if (isCurrent) 0.85f else 0.69f
                        )
                    )
                }
                ExpertStatus.LOADING -> {
                    ConduitProgressIndicator(
                        images = listOf(
                            painterResource(Res.drawable.plasma_s64),
                            painterResource(Res.drawable.plasma_s1)
                        ),
                        modifier = Modifier.size(28.dp),
                    )
                }
                ExpertStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Filled.PersonOff,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.15f),
                        modifier = Modifier.size(
                            iconBoxSize * if (isCurrent) 0.85f else 0.69f
                        )
                    )
                }
            }
        }

        Text(
            text = expert.nickname,
            style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow,),
            color = Color(0xFF007C91),
            maxLines = 1
        )
    }
}

@Composable
private fun CurrentExpertDecoration(
    color: Color,
    size: Dp
) {
    val outerRingSize = size
    val middleRingSize = size * 0.94f
    val innerRingSize = size * 0.88f
    val surfaceSize = size * 0.81f

    val infiniteTransition = rememberInfiniteTransition(label = "expertPulse")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(outerRingSize)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .border(1.dp, color.copy(alpha = 0.25f), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(middleRingSize)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .border(
                    1.5.dp,
                    color.copy(alpha = 0.55f),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(innerRingSize)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .border(
                    1.5.dp,
                    color.copy(alpha = 0.9f),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(surfaceSize)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.25f))
        )
    }
}
