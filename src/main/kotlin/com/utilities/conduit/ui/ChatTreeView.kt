package com.utilities.conduit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.utilities.conduit.AppState

@Composable
fun ChatTreeView(state: AppState) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .background(Color(0xFFEEEEEE))
    ) {
        Text(
            text = "Tree View Placeholder",
            modifier = Modifier.padding(10.dp)
        )
    }
}
