package com.utilities.conduit.packs

import com.utilities.conduit.AppState
import com.utilities.conduit.Expert
import com.utilities.conduit.ExpertStatus
import com.utilities.conduit.ExpertType
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
import com.utilities.conduit.utils.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

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

            // Internal system experts are always ready
            if (expert.type == ExpertType.INTERNAL) {
                expert.status = ExpertStatus.READY
                return@forEach
            }

            val modelFilename = expert.model
            if (modelFilename == null) {
                expert.status = ExpertStatus.FAILED
                return@forEach
            }

            val modelSha = appState.approvedModels.entries.firstOrNull { it.value.name == modelFilename }?.key
            if (modelSha == null) {
                expert.status = ExpertStatus.FAILED
                return@forEach
            }

            // The way model loading works is as follows:
            // the native module (libConduit) keeps a map of already loaded models keyed by their SHA
            // LlmPortal.initialize sends both the modelSha and an absoluteModelPath to the native side
            // to init. If it is already cached by SHA, then the given absoluteModelPath is IGNORED and
            // it could essentially be "". If it is not already loaded in the native cache, that is the
            // only case when it falls back to loading the raw file (when the absModelPath is actually used)

            val absoluteModelPath = AppUtils.locateModelFile(modelFilename) // returns null if not found
            appState.scope.launch(Dispatchers.IO) {
                // If path is given, it MUST match the SHA
                if (absoluteModelPath != null) {
                    val actualSha = sha256(File(absoluteModelPath))
                    if (modelSha != actualSha) {
                        expert.status = ExpertStatus.FAILED
                        return@launch
                    }
                }

                try {
                    expert.status = ExpertStatus.LOADING
                    //Trace.log("ABOUT TO INITIALIZE $modelFilename path=$absoluteModelPath sha=$modelSha")
                    expert.sessionPtr = LlmPortal.initialize(
                        conduitPtr = appState.conduitPtr,
                        modelSha = modelSha,
                        absoluteModelPath = absoluteModelPath ?: ""
                    )
                    //Trace.log("$absoluteModelPath got session pointer = ${expert.sessionPtr}")
                    expert.status = if (expert.sessionPtr == null) ExpertStatus.FAILED else ExpertStatus.READY
                } catch (e: Exception) {
                    expert.status = ExpertStatus.FAILED
                }
            }
        }
    }
}
