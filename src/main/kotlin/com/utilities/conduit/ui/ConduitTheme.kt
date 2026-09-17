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
    private val HelveticaNeueLight = FontFamily("Helvetica Neue")

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

// Used to give experts a UI color (and maybe in message bubbles... forgot)
val CONDUIT_COLORS = mapOf(
    "Proud Peacock"       to Color(0xFF007C91),
    "Misty Blue"          to Color(0xFF5C79DE),
    "Dusty Rose"          to Color(0xFFC56F88),
    "Muted Gold"          to Color(0xFFD0AD3F),
    "Cutey Peach"         to Color(0xFFD47C58),
    "Surprisingly Sage"   to Color(0xFF63A874),
    "Mr. Slater"          to Color(0xFF748E99),
    "Limpid Lavender"     to Color(0xFF917FB2),
    "Terracotta"          to Color(0xFFB96850),
    "Slippery Seafoam"    to Color(0xFF5FA695),
    "Pretty Periwinkle"   to Color(0xFF778CC0),
    "Arisi Mauve"         to Color(0xFFEEEBDE),
    "Gina Orangina"       to Color(0xFFF57C00),
    "Lemony Lu"           to Color(0xFFFFE000),
    "Scarlett Dawn"       to Color(0xFFE53935),
)
