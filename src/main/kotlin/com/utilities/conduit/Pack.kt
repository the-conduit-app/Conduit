package com.utilities.conduit

import com.utilities.conduit.ExpertTheme.getExpertColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    suspend fun initialize(state: AppState, scope: CoroutineScope) {
        withContext(Dispatchers.Main) {
            state.expertsMap.clear()
            state.expertsMap.putAll(experts.associateBy { it.id })
            state.currentPack.value = this@Pack
        }

        experts.forEach { expert ->
            scope.launch(Dispatchers.IO) { expert.initialize(state) }
        }
    }
}
