package com.utilities.conduit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileNotFoundException

// NOTE: Echo Expert is currently (July 2026) the only Internal Expert so
// watch out for some confusion below.

interface ExpertHandler {
    suspend fun initialize(expert: Expert, state: AppState)
    fun generateResponse(expert: Expert, state: AppState, messages: List<ChatMessage>): Flow<String>
}

object InternalExpertHandler : ExpertHandler {
    override suspend fun initialize(expert: Expert, state: AppState) {
        println("InternalExpertHandler: Initializing ${expert.nickname} -> ${expert.modelPath}") ////
        expert.status = ExpertStatus.READY
        // EchoExpert.initialize(expert) // Just do it here!
    }

    override fun generateResponse(expert: Expert, state: AppState, messages: List<ChatMessage>): Flow<String> {
        val prompt: String = buildPrompt(expert, state, messages)
        return EchoExpert.getResponse(expert, prompt)
    }

     private fun buildPrompt(expert: Expert, state: AppState, messages: List<ChatMessage>): String {
        return messages
            .lastOrNull { it.author.type == AuthorType.USER }
            ?.text
            ?: ""
    }
}

// ---------------------------------------------------------------------------------------------

object LlmExpertHandler : ExpertHandler {
    private val initMutex = Mutex()

    // Mainly File system checks before relaying init to LlmExpert.initialize()
    override suspend fun initialize(expert: Expert, state: AppState) {
        println("LlmExpertHandler.init(${expert.nickname}): ${expert.modelPath}") ////

        initMutex.withLock {
            if (expert.status == ExpertStatus.LOADING || expert.status == ExpertStatus.READY) {
                println("LlmExpertHandler.init: ${expert.nickname} has status ${expert.status}") ////
                return
            }
            if (expert.modelPath == null) return
            if (expert.status == ExpertStatus.FAILED) {
                println("LlmExpertHandler.init: ${expert.nickname} WILL NOT RETRY loading failed model ${expert.modelPath}")
                return
            }

            Expert.setStatusOfGroup(state, expert.modelPath, ExpertStatus.LOADING)
        }

        try {
            val absPathString = AppUtils.getAbsolutePathString(expert.modelPath!!)
            if (!File(absPathString).exists()) {
                throw FileNotFoundException("Model not found at: ${absPathString}")
            }

            expert.sessionPtr = LlmExpert.initialize(absPathString)
            Expert.setStatusOfGroup(state, expert.modelPath, ExpertStatus.READY, expert.sessionPtr)
        } catch (e: Exception) {
            Expert.setStatusOfGroup(state, expert.modelPath!!, ExpertStatus.FAILED)
            e.printStackTrace()
        }
        println("LlmExpertHandler.init: Done with ${expert.modelPath} ") ////
    }

    override fun generateResponse(expert: Expert, state: AppState, messages: List<ChatMessage>): Flow<String> {
        val prompt: String = buildPrompt(expert, state, messages)

        val session = expert.sessionPtr
            ?: throw IllegalStateException("Expert '${expert.nickname}' has not been initialized.")

        return LlmExpert.getResponse(session, prompt)
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
