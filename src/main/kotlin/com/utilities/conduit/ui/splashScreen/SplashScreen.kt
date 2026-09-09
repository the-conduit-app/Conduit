package com.utilities.conduit.ui.splashScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.utilities.conduit.AppState
import com.utilities.conduit.ConduitInitResult
import com.utilities.conduit.initializeConduit
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(appState: AppState, onReady: () -> Unit) {
    var conduitInitResult by remember { mutableStateOf<ConduitInitResult?>(null) }

    // Initialize Conduit.
    LaunchedEffect(Unit) {
        val modelFile = appState.systemExpert.model
        if (modelFile == null) {
            conduitInitResult = ConduitInitResult.FAILED_NO_MODELFILE  // Our problem
        } else {
            val absoluteModelPath = AppUtils.locateModelFile(modelFile)
            if (absoluteModelPath == null) {
                conduitInitResult = ConduitInitResult.FAILED_MODEL_MISSING // their problem
            } else {
                conduitInitResult = withContext(Dispatchers.IO) {
                    initializeConduit(appState, absoluteModelPath)
                }
            }
        }
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
