package com.utilities.conduit

import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
import com.utilities.conduit.utils.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private suspend fun initializeSystemExpert(modelPath: String): ConduitInitResult {
        val modelPath = state.systemExpert.modelPath
            ?: return ConduitInitResult.FAILED_NO_MODELPATH  // our problem

        val absoluteModelPath = findSystemExpertModel(modelPath)
            ?: return ConduitInitResult.FAILED_MODEL_MISSING // their problem

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

private const val GEMMA_SHA256 = "13b2a7b4115bbd0900162edcebe476da1ba1fc24e718e8b40d32f6e300f56dfe"

private fun findSystemExpertModel(modelPath: String): String? {
    val filename = File(modelPath).name

    val candidates = listOf(
        File(AppUtils.getAbsoluteModelPath(modelPath)),
        File(System.getProperty("user.home"), "llm/$filename"),
        File(System.getProperty("user.home"), "Models/$filename"),
        File(System.getProperty("user.home"), "models/$filename"),
        File(System.getProperty("user.home"), "Desktop/$filename")
    )

    return candidates.firstOrNull { file -> file.isFile && sha256(file) == GEMMA_SHA256 }
        ?.absolutePath
}

enum class ConduitInitResult {
    OK,
    FAILED_NO_PACK,
    FAILED_NO_MODELPATH,
    FAILED_MODEL_LOAD,
    FAILED_MODEL_MISSING,
    FAILED_MODEL_SHA
}
