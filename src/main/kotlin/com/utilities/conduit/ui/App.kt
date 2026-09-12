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
import com.utilities.conduit.maintenance.Maintenance
import com.utilities.conduit.maintenance.userActivityMonitor
import com.utilities.conduit.ui.chatView.FullMessagePanel
import conduit.generated.resources.Res
import conduit.generated.resources.plasma_s64
import io.github.fletchmckee.liquid.liquefiable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

val LocalActions = staticCompositionLocalOf<AppActions> { error("No AppActions provided") }

@Composable
fun App(appState: AppState) {
    val scope = rememberCoroutineScope()

    appState.scope.launch(Dispatchers.Default) {
        Maintenance.start(appState)
    }

    DisposableEffect(Unit) {
        val hook = Thread {
            scope.launch { appState.shutdown() }
        }
        Runtime.getRuntime().addShutdownHook(hook)

        onDispose {
            Runtime.getRuntime().removeShutdownHook(hook)
        }
    }

    //------------------------------------------------------------------------------------------

    CompositionLocalProvider(
        LocalActions provides AppActions(appState)
    ) {
        val appActions = LocalActions.current
        val liquidState = LocalLiquidState.current

        Box(
            Modifier
                .fillMaxSize()
                .userActivityMonitor(appState)
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
                MainScreen(appState)
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
