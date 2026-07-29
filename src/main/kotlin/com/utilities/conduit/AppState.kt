package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.sun.jna.Pointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppState(
    val scope: CoroutineScope,
    val chatManager: LiveChatManager,
    val currentExpert: MutableState<Expert?>,
    val currentPack: MutableState<Pack?>,
    val expertsMap: SnapshotStateMap<String, Expert>,
    val systemExpert: Expert?,
) {
    var chatsList by mutableStateOf(ChatsList())
    var availablePacks by mutableStateOf(emptyList<Pack>())

    companion object {
        // One new AppState per invocation
        fun createNew(scope: CoroutineScope): AppState {
            val systemExpert = Expert(
                id = "SYSTEM.ID", // SYSTEM.ID means "sticky" - model never evicted from cache
                type = ExpertType.LOCAL,
                nickname = "Internal Expert",
                expertise = "Various",
                ////modelPath = "llm/SmolLM2-135M-Instruct-Q4_K_M.gguf"
                ////modelPath = "llm/gemma-4-E4B-it-Q4_K_M.gguf"
                modelPath = "llm/gemma-2-9b-it-Q4_K_M.gguf"
            )

            return AppState(
                scope = scope,
                chatManager = LiveChatManager(scope, Chat.create("Welcome to Conduit")),
                currentExpert = mutableStateOf(null),
                currentPack = mutableStateOf(null),
                expertsMap = mutableStateMapOf(),
                systemExpert = systemExpert
            )
        }
    }

    suspend fun setStatusOfExpertGroup(
        modelPath: String,
        status: ExpertStatus,
        sessionPtr: Pointer? = null
    ) = withContext(Dispatchers.Main) {
        ////println("setStatusOfExpertGroup: $status")
        expertsMap.values.forEach { expert ->
            if (expert.modelPath == modelPath) {
                expert.status = status
                sessionPtr?.let { expert.sessionPtr = it }
            }
        }
    }
}
