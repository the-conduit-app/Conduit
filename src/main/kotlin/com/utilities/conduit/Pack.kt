package com.utilities.conduit

import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.file.Paths

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
        state.currentPack.value = this
    }

    suspend fun initializeExperts(state: AppState, scope: CoroutineScope) {
        experts.forEach { expert ->
            val modelPath = expert.modelPath ?: return@forEach
            if (expert.type != ExpertType.LOCAL) return@forEach

            val absoluteModelPath = AppUtils.getAbsoluteModelPath(modelPath)

            scope.launch(Dispatchers.IO) {
                expert.sessionPtr = LlmPortal.initialize(state.conduitPtr, absoluteModelPath)
            }
        }
    }
}
