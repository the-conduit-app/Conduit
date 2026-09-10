package com.utilities.conduit.ui.splashScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import com.utilities.conduit.AppState
import com.utilities.conduit.ConduitInitResult
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.initializeConduit
import com.utilities.conduit.ui.Sounds
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SplashScreen(appState: AppState, onReady: () -> Unit) {
    val scope = rememberCoroutineScope()
    val conduitInitResult = remember { mutableStateOf<ConduitInitResult?>(null) }
    val isInitializing = remember { mutableStateOf(false) }
    var isShaVerified by remember { mutableStateOf(false) }

    var isDraggingModel by remember { mutableStateOf(false) }

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { isDraggingModel = true }
            override fun onExited(event: DragAndDropEvent) { isDraggingModel = false }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDraggingModel = false

                if (isInitializing.value) return false

                val file = (event.dragData() as? DragData.FilesList)
                    ?.readFiles()
                    ?.firstOrNull()
                    ?.let { File(URI(it)) }
                    ?: return false

                scope.launch {
                    isInitializing.value = true
                    conduitInitResult.value = null
                    conduitInitResult.value = withContext(Dispatchers.IO) {
                        initializeConduit(appState, file.absolutePath, onVerified = { isShaVerified = true })
                    }
                    isInitializing.value = false
                }
                return true
            }
        }
    }

    LaunchedEffect(conduitInitResult.value) {
        if (conduitInitResult.value == null) {
            Sounds.Crunch.loop()
        } else {
            Sounds.Crunch.stop()
        }
    }

    // On App startup - Initialize Conduit.
    LaunchedEffect(Unit) {
        val modelFile = appState.systemExpert.model
        if (modelFile == null) {
            conduitInitResult.value = ConduitInitResult.FAILED_NO_MODELFILE  // Our problem
        } else {
            val absoluteModelPath = AppUtils.locateModelFile(modelFile)
            if (absoluteModelPath == null) {
                conduitInitResult.value = ConduitInitResult.FAILED_MODEL_MISSING // their problem
            } else {
                isInitializing.value = true
                conduitInitResult.value = withContext(Dispatchers.IO) {
                    initializeConduit(appState, absoluteModelPath, onVerified = { isShaVerified = true })
                }
                isInitializing.value = false
            }
        }
    }

    // Check every 5s if a system model file has become available in "common" locations.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000.milliseconds)

            if (conduitInitResult.value == ConduitInitResult.OK) { return@LaunchedEffect }
            if (isInitializing.value) { continue }
            val modelFile = appState.systemExpert.model ?: continue
            val absoluteModelPath = AppUtils.locateModelFile(modelFile) ?: continue

            isInitializing.value = true
            conduitInitResult.value = null
            Trace.log("Poller start init: initializeConduit = ${conduitInitResult.value}")

            val result = withContext(Dispatchers.IO) {
                initializeConduit(appState, absoluteModelPath, onVerified = { isShaVerified = true })
            }
            isInitializing.value = false

            Trace.log("Poller: initializeConduit returned $result")
            conduitInitResult.value = result
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .dragAndDropTarget(
                    shouldStartDragAndDrop = {
                        !isInitializing.value && conduitInitResult.value != ConduitInitResult.OK
                    },
                    target = dragAndDropTarget
                )
        ) {
            SplashSpinner(
                isReady = conduitInitResult.value == ConduitInitResult.OK,
                pulseDuration = 3000,
                isShaVerified = isShaVerified,
                onReady = onReady
            )
        }
    }
}
