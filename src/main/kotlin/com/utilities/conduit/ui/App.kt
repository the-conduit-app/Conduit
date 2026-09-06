package com.utilities.conduit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import com.utilities.conduit.*
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.LocalLiquidState
import com.utilities.conduit.utils.AppUtils
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s64
import io.github.fletchmckee.liquid.liquefiable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

// App level global constants
val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }
val AppJson = Json { prettyPrint = true; allowComments = true; ignoreUnknownKeys = true }

@Composable
fun App(state: AppState) {
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val hook = Thread {
            scope.launch { state.shutdown() }
        }
        Runtime.getRuntime().addShutdownHook(hook)

        onDispose {
            Runtime.getRuntime().removeShutdownHook(hook)
        }
    }

    //------------------------------------------------------------------------------------------

    CompositionLocalProvider(
        LocalActions provides AppActions(state)
    ) {
        val appActions = LocalActions.current
        val liquidState = LocalLiquidState.current

        Box(
            Modifier
                .fillMaxSize()
                .userActivityMonitor(state)
        ) {
            // Background
            Image(
                painter = painterResource(Res.drawable.plasma_s64),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(liquidState)
                    .zIndex(0f),
                contentScale = ContentScale.Crop
            )

            // Light wash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f))
                    .zIndex(0.1f)
            )

            // Application UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                MainScreen(state)
            }

            appActions.fullMessageText?.let { text ->
                FullMessagePanel(
                    modifier = Modifier.zIndex(2f),
                    text = text,
                    onDismiss = {
                        appActions.dismissFullMessage()
                    }
                )
            }
        }
    }
}
