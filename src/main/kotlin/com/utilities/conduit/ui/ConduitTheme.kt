package com.utilities.conduit.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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
