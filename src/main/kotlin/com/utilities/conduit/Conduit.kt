package com.utilities.conduit

import com.sun.beans.introspect.PropertyInfo
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.ConduitPortal
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.App
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

    appState.approvedModels =  getApprovedModels()

    appState.conduitUserModel = UserModel.loadConduitUserModelFromFile()
    appState.chatsList.build()

    val packs = withContext(Dispatchers.IO) { AppUtils.getAvailablePacks() }
    appState.availablePacks = packs
    val defaultPack = packs.find { it.id == "Default" } ?: return ConduitInitResult.FAILED_NO_PACK
    defaultPack.select(appState)

    val echoExpert = defaultPack.experts.find { it.model == InternalExperts.SIMPLE_ECHO }
    appState.currentExpert.value = echoExpert

    // ConduitExpert's hidden trails in Questopia
    ConduitPortal.initializeTrails()

    val conduitInitResult = initializeSystemExpert(appState, systemModelPath, onVerified)
    if (conduitInitResult != ConduitInitResult.OK) {
        return conduitInitResult
    }

    // Pack experts are non-core experts - they will all init in the bg. But it needs
    // to happen STRICTLY AFTER the system expert init to handle the case when it names
    // the right file, but it's missing!
    defaultPack.initializeExperts(appState)

    return conduitInitResult
}

// The System Expert must additionally match the trusted Gemma SHA-256.
private suspend fun initializeSystemExpert(
    appState: AppState,
    absoluteModelPath: String,
    onVerfified: () -> Unit
): ConduitInitResult {
    return try {
        if (sha256(File(absoluteModelPath)) != GEMMA_SHA256) {
            Trace.log("System model path = $absoluteModelPath, sha failed")
            return ConduitInitResult.FAILED_MODEL_SHA
        }
        onVerfified()

        appState.systemExpert.status = ExpertStatus.LOADING
        appState.systemExpert.sessionPtr = withContext(Dispatchers.IO) {
            LlmPortal.initialize(
                appState.conduitPtr,
                absoluteModelPath,
                GEMMA_SHA256
            )
        }
        appState.systemExpert.status =
            if (appState.systemExpert.sessionPtr == null) ExpertStatus.FAILED else ExpertStatus.READY

        return if (appState.systemExpert.sessionPtr == null) {
            ConduitInitResult.FAILED_MODEL_LOAD
        } else {
            ConduitInitResult.OK
        }
    } catch (e: Exception) {
        Trace.log("System expert initialization failed: ${e.message}")
        ConduitInitResult.FAILED_MODEL_LOAD
    }
}

private fun getApprovedModels(): Map<String, ApprovedModel> {
    val file = File(AppUtils.getAppDir(), ".approved-models.json")

    return if (file.isFile) {
        try {
            AppJson.decodeFromString<Map<String, ApprovedModel>>(file.readText())
        } catch (e: Exception) {
            Trace.log("Unable to load .approved-models.json: ${e.message}")
            emptyMap()
        }
    } else {
        emptyMap()
    }
}
