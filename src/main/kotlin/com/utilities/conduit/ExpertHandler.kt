package com.utilities.conduit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

// NOTE: Echo Expert is currently (July 2026) the only Internal Expert so
// watch out for some confusion below.

interface ExpertHandler {
    suspend fun initialize(expert: Expert, state: AppState)
    fun generateResponse(expert: Expert, state: AppState, messages: List<ChatMessage>): Flow<String>
    fun abortResponse(expert: Expert)
}

// ---------------------------------------------------------------------

object InternalExpertHandler : ExpertHandler {
    override suspend fun initialize(expert: Expert, state: AppState) {
        withContext(Dispatchers.Main) { expert.status = ExpertStatus.READY }
    }

    override fun generateResponse(expert: Expert, state: AppState, messages: List<ChatMessage>): Flow<String> {
        val prompt: String = buildPrompt(expert, state, messages)
        return EchoPortal.getResponse(expert, prompt)
    }

    override fun abortResponse(expert: Expert) {
        println("InternalExpertHandler: Aborting ${expert.modelPath}") ////
        EchoPortal.abortResponse(expert)
    }

     private fun buildPrompt(expert: Expert, state: AppState, messages: List<ChatMessage>): String {
        return messages
            .lastOrNull { it.author.type == AuthorType.USER }
            ?.text
            ?: ""
    }
}

// ---------------------------------------------------------------------------------------------

object LocalExpertHandler : ExpertHandler {
    private val initMutex = Mutex()

    // Mainly File system checks before relaying init to LlmExpert.initialize()
    override suspend fun initialize(expert: Expert, state: AppState) {
        println("LocalExpertHandler Initializing ${expert.nickname} ${System.identityHashCode(expert)}")
        initMutex.withLock {
            if (expert.status == ExpertStatus.LOADING || expert.status == ExpertStatus.READY) {
                return
            }
            if (expert.modelPath == null) return
            if (expert.status == ExpertStatus.FAILED) {
                return
            }

            state.setStatusOfExpertGroup(expert.modelPath, ExpertStatus.LOADING)
        }

        try {
            val absPathString = AppUtils.getAbsolutePathString(expert.modelPath!!)
            if (!File(absPathString).exists()) {
                throw FileNotFoundException("Model not found at: ${absPathString}")
            }

            expert.sessionPtr = LlmPortal.initialize(absPathString)
            state.setStatusOfExpertGroup(expert.modelPath, ExpertStatus.READY, expert.sessionPtr)
        } catch (e: Exception) {
            state.setStatusOfExpertGroup(expert.modelPath!!, ExpertStatus.FAILED)
            e.printStackTrace()

        }
        println("LlmExpertHandler.init: Done with ${expert.modelPath} ") ////
    }

    override fun generateResponse(expert: Expert, state: AppState, messages: List<ChatMessage>): Flow<String> {
        val prompt: String = buildPrompt(expert, state, messages)

        val session = expert.sessionPtr
            ?: throw IllegalStateException("Expert '${expert.nickname}' has not been initialized.")

        return LlmPortal.getResponse(session, prompt)
    }

    override fun abortResponse(expert: Expert) {
        LlmPortal.abortResponse(expert)
    }

    private fun buildPrompt(expert: Expert, state: AppState, messages: List<ChatMessage>): String {
        return buildString {

            expert.seedPrompt
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append("<|im_start|>system\n")
                    append(it)
                    append("\n<|im_end|>\n")
                }

            messages.forEach { msg ->
                val role = when (msg.author.type) {
                    AuthorType.USER      -> "user"
                    AuthorType.ASSISTANT -> "assistant"
                    AuthorType.CONDUIT    -> "system"
                }

                append("<|im_start|>")
                append(role)
                append('\n')
                append(msg.text)
                append("\n<|im_end|>\n")
            }

            append("<|im_start|>assistant\n")
        }
    }
}
