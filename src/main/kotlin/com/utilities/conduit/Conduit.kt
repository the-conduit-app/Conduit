package com.utilities.conduit

import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.Sounds
import com.utilities.conduit.utils.AppUtils
import com.utilities.conduit.utils.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val GEMMA_SHA256 = "13b2a7b4115bbd0900162edcebe476da1ba1fc24e718e8b40d32f6e300f56dfe"

enum class ConduitInitResult {
    OK,
    FAILED_NO_PACK,
    FAILED_NO_MODELFILE,
    FAILED_MODEL_LOAD,
    FAILED_MODEL_MISSING,
    FAILED_MODEL_SHA
}

suspend fun initializeConduit(
    appState: AppState,
    systemModelPath: String?,
    onVerified: () -> Unit = {}
): ConduitInitResult {
    if (systemModelPath == null)
        return ConduitInitResult.FAILED_MODEL_MISSING
    Trace.log("system model path = $systemModelPath")

    appState.conduitUserModel = UserModel.loadConduitUserModelFromFile()
    appState.chatsList.build()

    val packs = withContext(Dispatchers.IO) { AppUtils.getAvailablePacks() }
    appState.availablePacks = packs
    val defaultPack = packs.find { it.id == "Default" } ?: return ConduitInitResult.FAILED_NO_PACK
    defaultPack.select(appState)

    val echoExpert = defaultPack.experts.find { it.model == InternalExperts.SIMPLE_ECHO }
    appState.currentExpert.value = echoExpert

    // Pack experts are non-core experts - they will all init in the bg.
    defaultPack.initializeExperts(appState)

    // start off maint in the bg
    appState.scope.launch { Maintenance.start(appState) }

    return initializeSystemExpert(appState, systemModelPath, onVerified)
}

// The System Expert must additionally match the trusted Gemma SHA-256.
private suspend fun initializeSystemExpert(
    appState: AppState,
    absoluteModelPath: String,
    onVerfified: () -> Unit
): ConduitInitResult {
    return try {
        if (sha256(File(absoluteModelPath)) != GEMMA_SHA256) return ConduitInitResult.FAILED_MODEL_SHA
        onVerfified()

        appState.systemExpert.sessionPtr = withContext(Dispatchers.IO) {
            LlmPortal.initialize(
                appState.conduitPtr,
                absoluteModelPath
            )
        }
        ConduitInitResult.OK
    } catch (e: Exception) {
        Trace.log("System expert initialization failed: ${e.message}")
        ConduitInitResult.FAILED_MODEL_LOAD
    }
}
