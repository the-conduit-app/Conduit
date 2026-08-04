package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppState(
    val scope: CoroutineScope,
    val chatManager: ChatManager,
    val currentExpert: MutableState<Expert?>,
    val currentPack: MutableState<Pack?>,
    val expertsMap: SnapshotStateMap<String, Expert>,
    val systemExpert: Expert,
) {
    var chatsList by mutableStateOf(ChatsList())
    var availablePacks by mutableStateOf(emptyList<Pack>())
    val notification = Notification(scope)
    var leftPanelMode by mutableStateOf(LeftPanelMode.LIST)

    private val modelStates = mutableMapOf<String, ModelState>()
    fun getModelState(modelPath: String): ModelState = modelStates.getOrPut(modelPath) {
        ModelState(modelPath)
    }

    companion object {
        val systemExpert = Expert(
            id = "SYSTEM.ID",
            type = ExpertType.LOCAL,
            nickname = "Conduit",
            expertise = "General",
            modelPath = "llm/gemma-2-9b-it-Q4_K_M.gguf"
        )

        // One new AppState per invocation
        fun createNew(scope: CoroutineScope): AppState {
            val state: AppState = AppState(
                scope = scope,
                chatManager = ChatManager(scope),
                currentExpert = mutableStateOf(null),
                currentPack = mutableStateOf(null),
                expertsMap = mutableStateMapOf(),
                systemExpert = systemExpert
            )
            state.systemExpert.modelState = state.getModelState(state.systemExpert.modelPath!!)
            return state
        }
    }

    suspend fun shutdown() = withContext(Dispatchers.IO) {
        //Trace.log("AppState.shutdown() ENTER")
        //Trace.log("ModelStates = ${modelStates.size}")

        modelStates.values.forEach {
            //Trace.log("Shutdown start: ${it.modelPath}")
            //Trace.log("${it.modelPath}.status = " + it.status  )
            it.shutdown()
            //Trace.log("Shutdown done: ${it.modelPath}")
        }

        //Trace.log("Calling LlmPortal.shutdown()")
        LlmPortal.shutdown()

        //Trace.log("AppState.shutdown() EXIT")
    }
}
