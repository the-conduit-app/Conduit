package com.utilities.conduit

import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileNotFoundException

interface ExpertHandler {
    suspend fun initialize(expert: Expert, state: AppState)
    fun getResponse(expert: Expert, state: AppState, prompt: String): Flow<String>

    companion object {
        fun createNew(expert: Expert): ExpertHandler {
            return when (expert.type) {
                ExpertType.INTERNAL -> InternalExpertHandler()
                ExpertType.LOCAL -> LlmExpertHandler()
                ExpertType.REMOTE -> throw NotImplementedError("Remote experts not yet supported")
            }
        }
    }
}

class InternalExpertHandler : ExpertHandler {
    override suspend fun initialize(expert: Expert, state: AppState) {
        println("Expert Handler (Internal): Initializing ${expert.modelPath}") ////
        EchoExpert.initialize(expert)
    }

    override fun getResponse(expert: Expert, state: AppState, prompt: String): Flow<String> {
        return EchoExpert.getResponse(expert, prompt)
    }
}

// ---------------------------------------------------------------------------------------------

class LlmExpertHandler : ExpertHandler {

    // Mainly File system checks before relaying init to LlmExpert.initialize()
    override suspend fun initialize(expert: Expert, state: AppState) {
        println("LlmExpertHandler.init: ${expert.modelPath}") ////

        if (expert.status == ExpertStatus.LOADING || expert.status == ExpertStatus.READY) return
        if (expert.modelPath == null) return
        if (expert.status == ExpertStatus.FAILED) {
            println("LlmExpertHandler.init: ${expert.nickname} WILL NOT RETRY loading failed model ${expert.modelPath}")
            return
        }

        Expert.setStatusOfGroup(state, expert.modelPath, ExpertStatus.LOADING)
        try {
            val absPathString = if (expert.modelPath.startsWith("/")) expert.modelPath else "${AppUtils.getAppPath()}/${expert.modelPath}"
            if (!File(absPathString).exists()) {
                throw FileNotFoundException("Model not found at: ${absPathString}")
            }

            LlmExpert.initialize(absPathString)
            Expert.setStatusOfGroup(state, expert.modelPath, ExpertStatus.READY)
        } catch (e: Exception) {
            Expert.setStatusOfGroup(state, expert.modelPath, ExpertStatus.FAILED)
            e.printStackTrace()
        }
        println("LlmExpertHandler.init: Done with ${expert.modelPath} ") ////
    }

    override fun getResponse(expert: Expert, state: AppState, prompt: String): Flow<String> {
        return LlmExpert.getResponse(expert, prompt)
    }
}
