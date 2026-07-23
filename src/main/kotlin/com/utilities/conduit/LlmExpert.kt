package com.utilities.conduit

import com.sun.jna.Pointer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class LlmExpert {
    companion object {
        private val conduitLibPath = AppUtils.getNativeLibPath("libconduit.dylib")

        val conduitLib: ConduitLib =
            com.sun.jna.Native.load(
                conduitLibPath,
                ConduitLib::class.java
            )

        init {
            conduitLib.conduit_llm_init() // for llama_backend init
        }

        val sessionCache = mutableMapOf<String, Pointer>()

        fun shutdown() {
            println("LlmExpert.shutdown: releasing ${sessionCache.size} sessions")

            sessionCache.forEach { (name, session) ->
                println("Freeing session: $name")
                conduitLib.conduit_llm_free_session(session)
            }

            sessionCache.clear()

            println("Freeing llama backend")
            conduitLib.conduit_llm_free()

            println("LlmExpert.shutdown: complete")
        }

        private fun requireSession(path: String): Pointer =
            sessionCache[path] ?: error("No session loaded for $path")

        // ---------------------------------------------------------------------------------
        // Potentially time-consuming - Ensure it's within IO Thread
        fun initialize(absoluteModelPath: String)  {
            if (!File(absoluteModelPath).exists()) {
                throw IllegalArgumentException("Model file not found at: ${absoluteModelPath}")
            }
            sessionCache[absoluteModelPath]?.let {
                println("LlmExpert.init: Using existing session for $absoluteModelPath")
                return
            }

            sessionCache[absoluteModelPath] = conduitLib.conduit_llm_get_session(absoluteModelPath)
                ?: throw RuntimeException("Failed to Load: ${absoluteModelPath}")

            println("LlmExpert.init: Successfully created session for $absoluteModelPath") ////
        }

        // ---------------------------------------------------------------------------------
        fun getResponse(expert: Expert, prompt: String): Flow<String> = callbackFlow {
            val absModelPath = requireNotNull(AppUtils.getAbsolutePathString(expert.modelPath?: ""))

            val sessionPtr = requireSession(absModelPath)
            val callback = object : ConduitTokenCallback {
                override fun invoke(piece: String?, userData: Pointer?) {
                    if (piece != null)
                        trySend(piece)
                }
            }

            val rc = conduitLib.conduit_session_generate(sessionPtr, prompt, callback, null)
            if (rc != 0) {
                close(RuntimeException("Generation failed (rc=$rc)"))
            } else {
                close()
            }
        }

        /* TODO - Obsolete
        fun XgetResponse(modelPtr: Long, prompt: String, ctxParams: LlamaBridge.LlamaContextParams): Flow<String> = flow {
            val ctxPtr = LlamaBridge.INSTANCE.llama_new_context_with_model(modelPtr, ctxParams)
            val tokenBuffer = IntArray(ctxParams.n_ctx)

            try {
                val numPromptTokens = LlamaBridge.INSTANCE.llama_tokenize(
                    modelPtr, prompt, tokenBuffer, ctxParams.n_ctx, true
                )

                LlamaBridge.INSTANCE.llama_eval(ctxPtr, tokenBuffer, numPromptTokens, 0, ctxParams.n_threads)

                var nPast = numPromptTokens
                val llamaTokenDataArray = LlamaBridge.LlamaTokenDataArray()
                val eosToken = LlamaBridge.INSTANCE.llama_token_eos(modelPtr)

                repeat(ctxParams.n_ctx - numPromptTokens) {
                    if (nPast >= ctxParams.n_ctx - 1) return@flow

                    val nextToken = LlamaBridge.INSTANCE.llama_sample_token_greedy(ctxPtr, llamaTokenDataArray)
                    if (nextToken == eosToken) return@flow

                    emit(LlamaBridge.INSTANCE.llama_token_to_piece(ctxPtr, nextToken))

                    tokenBuffer[0] = nextToken
                    LlamaBridge.INSTANCE.llama_eval(ctxPtr, tokenBuffer, 1, nPast, ctxParams.n_threads)
                    nPast += 1
                }
            } finally {
                LlamaBridge.INSTANCE.llama_free(ctxPtr) // Note - only releases the context, not the model itself
            }
        }
        */
    }
}
