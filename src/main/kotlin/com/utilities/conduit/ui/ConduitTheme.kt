package com.utilities.conduit.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ConduitTheme {
    @OptIn(ExperimentalTextApi::class)
    private val HelveticaNeueLight = FontFamily(
        "Helvetica Neue"
    )

    val Typography = Typography(
        bodyLarge = TextStyle(
            fontFamily = HelveticaNeueLight,
            fontWeight = FontWeight.Light
        ),
        bodyMedium = TextStyle(
            fontFamily = HelveticaNeueLight,
            fontWeight = FontWeight.Light
        ),
        bodySmall = TextStyle(
            fontFamily = HelveticaNeueLight,
            fontWeight = FontWeight.Light
        ),
        titleLarge = TextStyle(
            fontFamily = HelveticaNeueLight,
            fontWeight = FontWeight.Light,
            fontSize = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = HelveticaNeueLight,
            fontWeight = FontWeight.Light,
            fontSize = 18.sp
        ),
        titleSmall = TextStyle(
            fontFamily = HelveticaNeueLight,
            fontWeight = FontWeight.Light,
        )
    )
}

@Composable
fun ConduitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = ConduitTheme.Typography,
        content = content
    )
}

val CONDUIT_COLORS = mapOf(
    "Proud Peacock" to Color(0xFF007C91),
    "Misty Blue" to Color(0xFF8DB9CC),
    "Dusty Rose" to Color(0xFFD09AAA),
    "Muted Gold" to Color(0xFFD5BF72),
    "Cutey Peach" to Color(0xFFD5A084),
    "Surprisingly Sage" to Color(0xFF91B99A),
    "Mr. Slater" to Color(0xFF9EADB3),
    "Limpid Lavender" to Color(0xFFB3A6C7),
    "Terracotta" to Color(0xFFC58B78),
    "Slippery Seafoam" to Color(0xFF8FB9AD),
    "Pretty Periwinkle" to Color(0xFF9FAED0),
    "Arisi Mauve" to Color(0xFFF5F0E6),
    "Gina Orangina" to Color(0xFFF57C00),
    "Lemony Lu" to Color(0xFDD835),
    "Scarlett Dawn" to Color(0xE53935)
)
