package com.utilities.conduit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.utilities.conduit.Pack
import com.utilities.conduit.expertColors
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s1
import org.jetbrains.compose.resources.painterResource

@Composable
fun PackCard(
    pack: Pack,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(0xFF007C91)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                accent.copy(alpha = 0.10f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected)
                accent
            else
                MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box {
            Image(
                painter = painterResource(Res.drawable.plasma_s1),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = if (isSelected) 0.5f else 0.75f),
                shape = RoundedCornerShape(7.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pack.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isSelected)
                                accent
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = accent
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    pack.experts.forEach { expert ->
                        val color = expertColors[expert.color] ?: Color.Gray

                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 1.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "${expert.nickname} (${expert.expertise})",
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Text(
                                    text = expert.description ?: "An expert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
