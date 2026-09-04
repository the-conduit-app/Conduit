package com.utilities.conduit.ui.splashScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingToolbarDefaults.animationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.utilities.conduit.ui.Sounds
import kotlinx.coroutines.delay
import java.lang.System.exit
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        Sounds.Space.play()
    }

    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None, // fadeIn(animationSpec = tween(800)),
        exit = fadeOut(animationSpec = tween(800))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { visible = false },
            ) {
                Text("Continue")
            }
        }
    }

    // Once the fade-out has completed, remove the overlay.
    LaunchedEffect(visible) {
        if (!visible) {
            //println("Stopping space sound")
            Sounds.Space.stop()
            //delay(1000.milliseconds)
            Sounds.EnterChime.play()
            //delay(500.milliseconds)

            onDismiss()
        }
    }
}
