package com.utilities.conduit

import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class Conduit(private val state: AppState) {

    suspend fun initialize(): ConduitInitResult {
        state.conduitUserModel = UserModel.loadConduitUserModelFromFile()
        state.chatsList.build()

        val packs = withContext(Dispatchers.IO) { AppUtils.getAvailablePacks() }
        state.availablePacks = packs
        val defaultPack = packs.find { it.id == "Default" } ?: return ConduitInitResult.FAILED_NO_PACK
        defaultPack.select(state)

        val echoExpert = defaultPack.experts.find { it.modelPath == InternalExperts.SIMPLE_ECHO }
        state.currentExpert.value = echoExpert

        // Pack experts are non-core experts - they will all init in the bg.
        defaultPack.initializeExperts(state)

        // start off maint in the bg
        state.scope.launch { Maintenance.start(state) }

        return initializeSystemExpert()
    }

    private suspend fun initializeSystemExpert(): ConduitInitResult {
        val modelPath = state.systemExpert.modelPath ?: return ConduitInitResult.FAILED_NO_MODELPATH
        val absoluteModelPath = AppUtils.getAbsoluteModelPath(modelPath)

        return try {
            state.systemExpert.sessionPtr = withContext(Dispatchers.IO) {
                LlmPortal.initialize(
                    state.conduitPtr,
                    absoluteModelPath
                )
            }
            ConduitInitResult.OK
        } catch (e: Exception) {
            Trace.log("System expert initialization failed: ${e.message}")
            ConduitInitResult.FAILED_MODEL_LOAD
        }
    }
}

enum class ConduitInitResult {
    OK,
    FAILED_NO_PACK,
    FAILED_NO_MODELPATH,
    FAILED_MODEL_LOAD
}
