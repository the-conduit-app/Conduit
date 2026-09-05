package com.utilities.conduit.ui.splashScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState
import com.utilities.conduit.Conduit
import com.utilities.conduit.ConduitInitResult
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState

@Composable
fun SplashScreen(
    state: AppState,
    onReady: () -> Unit
) {
    var conduitInitResult by remember { mutableStateOf<ConduitInitResult?>(null) }

    // Initialize Conduit.
    LaunchedEffect(Unit) {
        val conduit = Conduit(state)
        conduitInitResult = conduit.initialize()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        SplashSpinner(
            isReady = conduitInitResult == ConduitInitResult.OK,
            pulseDuration = 3000,
            onReady = onReady,
        )
    }
}
