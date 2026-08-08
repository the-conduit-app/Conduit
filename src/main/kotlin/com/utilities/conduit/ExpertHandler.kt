package com.utilities.conduit

import kotlinx.coroutines.flow.Flow

// NOTE: Echo Expert is currently (July 2026) the only Internal Expert

interface ExpertHandler {
    fun generateResponse(expert: Expert, messages: List<ChatMessage>): Flow<String>
    fun abortResponse(expert: Expert)
}

// ---------------------------------------------------------------------

object InternalExpertHandler : ExpertHandler {
    override fun generateResponse(expert: Expert, messages: List<ChatMessage>): Flow<String> {
        val prompt = buildPrompt(expert, messages)
        return EchoPortal.getResponse(expert, prompt)
    }

    override fun abortResponse(expert: Expert) {
        EchoPortal.abortResponse()
    }

    // Unused param expert
    private fun buildPrompt(expert: Expert, messages: List<ChatMessage>): String {
        return messages
            .lastOrNull { it.author.type == AuthorType.USER }
            ?.text
            ?: ""
    }
}

// ---------------------------------------------------------------------------------------------

object LocalExpertHandler : ExpertHandler {
    override fun generateResponse(expert: Expert, messages: List<ChatMessage>): Flow<String> {
        val sessionPtr = expert.sessionPtr ?: error("Expert '${expert.nickname}': LLM session not found.")

        val prompt: String = buildPrompt(expert, messages)
        return LlmPortal.getResponse(sessionPtr, prompt)
    }

    override fun abortResponse(expert: Expert) {
        expert.sessionPtr?.let {
            LlmPortal.abortResponse(it)
        }
    }

    private fun buildPrompt(expert: Expert, messages: List<ChatMessage>): String {
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
                    AuthorType.SYSTEM    -> "system"
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
