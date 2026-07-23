package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ExpertType { INTERNAL, LOCAL, REMOTE }
enum class ExpertStatus { NONE, LOADING, READY, FAILED }

@Serializable
class Expert(
    val id: String,
    val type: ExpertType,
    val nickname: String,
    val expertise: String,
    val modelPath: String? = null,
    val seedPrompt: String? = null,
    ) {
    @Transient
    val handler: ExpertHandler = ExpertHandler.createNew(this) // TODO - catch failure
    var status by mutableStateOf(ExpertStatus.NONE)
    val color: Color
        get() = ExpertTheme.getExpertColor(id)

    companion object {
        fun setStatusOfGroup(state: AppState, modelPath: String, status: ExpertStatus) {
            CoroutineScope(Dispatchers.Main).launch {
                state.expertsMap.values.forEach { expert ->
                    if (expert.modelPath == modelPath) {
                        expert.status = status
                    }
                }
            }
        }

        object ExpertTheme {
            private val colors = listOf(
                Color(0xFF81D4FA), // Sky Blue
                Color(0xFFF48FB1), // Vivid Pink
                Color(0xFFFBC02D), // Bright Yellow
                Color(0xFFFFAB91), // Vibrant Orange
                Color(0xFFA5D6A7), // Soft Green
                Color(0xFFB0BEC5)  // Slate Gray
            )
            fun getExpertColor(expertId: String): Color {
                return colors[Math.abs(expertId.hashCode()) % colors.size]
            }
        }
    }

    suspend fun initialize(state: AppState) {
        println("Expert (Init): ${this.nickname}")
        handler.initialize(this, state)
    }

    fun getResponse(state: AppState, prompt: String): Flow<String> {
        return handler.getResponse(this, state, prompt)
    }
}
