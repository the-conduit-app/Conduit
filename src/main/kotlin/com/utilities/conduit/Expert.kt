package com.utilities.conduit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

enum class ExpertType { ECHO, LOCAL, REMOTE }
enum class ExpertStatus { NONE, LOADING, READY, FAILED }

@Serializable
data class Expert(
    val id: String,
    val type: ExpertType,
    val nickname: String,
    val expertise: String,
    val modelPath: String? = null,
    val seedPrompt: String? = null,
    ) {
    var status by mutableStateOf(ExpertStatus.NONE)
    val color: Color
        get() = ExpertTheme.getColorForExpert(id)

    companion object {
        private val GlobalModelCache = java.util.concurrent.ConcurrentHashMap<String, Any>()

        fun clearModelFromCache(modelPath: String?) {
            if (modelPath != null) GlobalModelCache.remove(modelPath)
        }

        fun clearUnusedModels(usedModelPaths: Set<String>) {
            GlobalModelCache.keys.removeIf { path -> !usedModelPaths.contains(path) }
        }

        fun setStatusOfGroup(state: AppState, modelPath: String, status: ExpertStatus) {
            CoroutineScope(Dispatchers.Main).launch {
                state.expertsMap.values.forEach { expert ->
                    if (expert.modelPath == modelPath) {
                        expert.status = status
                    }
                }
            }
        }
    }

    // Potentially time-consuming: MUST BE invoked within Dispatchers.IO
    suspend fun initialize(state: AppState) {
        if (modelPath.isNullOrBlank() || status == ExpertStatus.LOADING || status == ExpertStatus.READY)
            return

        withContext(Dispatchers.IO) {
            if (type == ExpertType.LOCAL && GlobalModelCache.containsKey(modelPath)) {
                setStatusOfGroup(state, modelPath, ExpertStatus.READY)
                return@withContext
            }

            // All experts sharing the same model file now flip to LOADING
            setStatusOfGroup(state, modelPath, ExpertStatus.LOADING)

            try {
                if (type == ExpertType.LOCAL) {
                    val currentPackPaths = state.currentPack.value?.experts?.mapNotNull { it.modelPath }?.toSet() ?: emptySet()
                    Expert.clearUnusedModels(currentPackPaths) // Make space
                    GlobalModelCache.computeIfAbsent(modelPath) { path -> initLLM(path) }
                }
                setStatusOfGroup(state, modelPath, ExpertStatus.READY)
            } catch (e: Exception) {
                setStatusOfGroup(state, modelPath, ExpertStatus.FAILED)
                e.printStackTrace()
            }
        }
    }

    // TIME CONSUMING, BLOCKING - Load the given GGUF file (was not in cache)
    private fun initLLM(path: String): LlamaModel {
        val homeDir = System.getProperty("user.home")
        val absolutePath = if (path.startsWith("~")) { path.replaceFirst("~", homeDir) } else { path }

        val modelFile = File(absolutePath)
        if (!modelFile.exists()) {
            throw IllegalArgumentException("Model file not found at: ${modelFile.absolutePath}")
        }

        val modelParams = ModelParameters().setModel(modelFile.absolutePath).setThreads(4)
        return LlamaModel(modelParams)
    }

    // --------------------------------------------------------------------------------

    object ExpertTheme {
        private val colors = listOf(
            Color(0xFF81D4FA), // Sky Blue
            Color(0xFFF48FB1), // Vivid Pink
            Color(0xFFFBC02D), // Bright Yellow
            Color(0xFFFFAB91), // Vibrant Orange
            Color(0xFFA5D6A7), // Soft Green
            Color(0xFFB0BEC5)  // Slate Gray
        )

        fun getColorForExpert(expertId: String): Color {
            return colors[Math.abs(expertId.hashCode()) % colors.size]
        }
    }

    // --------------------------------------------------------------------------------

    fun getResponse(messages: List<ChatMessage>): Flow<String> {
        return when (this.type) {
            ExpertType.ECHO -> responseFromEcho(messages)
            ExpertType.LOCAL -> responseFromLocalAI(messages)
            ExpertType.REMOTE -> flowOf("REMOTE generation pending.")
        }
    }

    private fun responseFromEcho(messages: List<ChatMessage>): Flow<String> = flow {
        val response = "Echoing: " + (messages.lastOrNull()?.text ?: "Silence.")
        for (char in response) {
            emit(char.toString())
            kotlinx.coroutines.delay(50.milliseconds) // Small delay to visualize the stream
        }
    }

    private fun responseFromLocalAI(messages: List<ChatMessage>): Flow<String> = flow {
        val model = GlobalModelCache[modelPath] as? LlamaModel
            ?: throw IllegalStateException("Model not yet initialized")

        val prompt = "User: ${messages.last().text}\nAssistant:"
        val inferenceParams = InferenceParameters(prompt).setTemperature(0.7f)

        for (output in model.generate(inferenceParams)) {
            emit(output.text)
        }
    }
}
