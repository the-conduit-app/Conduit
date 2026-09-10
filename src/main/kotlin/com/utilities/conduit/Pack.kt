package com.utilities.conduit

import com.sun.beans.introspect.PropertyInfo
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
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
        state.currentPack.value = this
    }

    // Non-blocking - Launches bg inits for the various experts in the pack
    fun initializeExperts(appState: AppState) {
        experts.forEach { expert ->
            expert.setConduitUserModelGetter() {
                appState.conduitUserModel
            }

            if (expert.type != ExpertType.LLM) return@forEach

            val modelFilename = expert.model ?: return@forEach
            val modelSha = appState.approvedModels.entries
                .firstOrNull { it.value.name == modelFilename }
                ?.key ?: return@forEach

            val absoluteModelPath = AppUtils.locateModelFile(modelFilename)
                appState.scope.launch(Dispatchers.IO) {
                    expert.sessionPtr = LlmPortal.initialize(
                        conduitPtr =  appState.conduitPtr,
                        modelSha = modelSha,
                        absoluteModelPath = absoluteModelPath ?: ""
                    )
                }
        }
    }
}
