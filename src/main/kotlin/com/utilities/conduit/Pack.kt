package com.utilities.conduit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Pack(
    @Transient
    val id: String = "", // Note: Not UUID. Set to the filename sans .json in getAvailablePacks()

    val name: String = "",
    val description: String = "",
    val experts: List<Expert>
) {
    fun select(state: AppState) {
        state.expertsMap.clear()
        state.expertsMap.putAll(experts.associateBy { it.id })

        experts.forEach { expert ->
            val modelPath = expert.modelPath ?: return@forEach
            expert.modelState = state.getModelState(modelPath)
        }

        state.currentPack.value = this
    }

    suspend fun initializeExperts(state: AppState, scope: CoroutineScope) {
        experts.forEach { expert ->
            scope.launch(Dispatchers.IO) { expert.modelState?.initialize() }
        }
    }
}
