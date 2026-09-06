package com.utilities.conduit.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.fletchmckee.liquid.LiquidState

val LocalLiquidState = staticCompositionLocalOf<LiquidState> {
    error("LiquidState not provided")
}
