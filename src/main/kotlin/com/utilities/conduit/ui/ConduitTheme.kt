package com.utilities.conduit.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ConduitTheme {

    object Colors {

        object MessageBubble {
            val User = Color(0xFFDCF8C6)
            val Expert = Color(0xFFFFF9C4)

            object Branchable {
                val User = Color(0xFFB7D9A8)
                val Expert = Color(0xFFFFE8B0)
            }
        }
    }

    object Dimensions {
        object MessageBubble {
            val CornerRadius = 12.dp
            val BranchableCornerRadius = 14.dp
            val ShadowElevation = 6.dp
            val Padding = 11.dp
            val MaxWidth = 700.dp
        }
    }
}
