package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CoroutineScope

class AppState(
    val chatManager: LiveChatManager,
    val currentExpert: MutableState<Expert?>,
    val currentPack: MutableState<Pack?>,
    val expertsMap: SnapshotStateMap<String, Expert>,
    val systemExpert: Expert?
) {
    var pastChatsInfo = mutableStateMapOf<String, ChatInfo>()
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
                chatManager = LiveChatManager(scope, Chat.create("Welcome to Conduit")),
                currentExpert = mutableStateOf(null),
                currentPack = mutableStateOf(null),
                expertsMap = mutableStateMapOf(),
                systemExpert = systemExpert
            )
        }
    }
}
