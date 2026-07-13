package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

class AppState(
    val chatManager: LiveChatManager,
    val currentExpert: MutableState<Expert?>,
    val currentPack: MutableState<Pack?>,
    val expertsMap: SnapshotStateMap<String, Expert>
) {
    companion object {
        fun createNew(): AppState { // One new AppState per invocation
            return AppState(
                chatManager = LiveChatManager(Chat.create("Welcome to Conduit")),
                currentExpert = mutableStateOf(null),
                currentPack = mutableStateOf(null),
                expertsMap = mutableStateMapOf()
            )
        }
    }

    fun setCurrentExpert(expert: Expert?) {
        currentExpert.value = expert
    }
}
