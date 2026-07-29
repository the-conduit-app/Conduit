package com.utilities.conduit

import com.sun.jna.Pointer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class LlmPortal {
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
        fun getSession(path: String): Pointer =
            sessionCache[path] ?: error("No session loaded for $path")

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
        fun initialize(absoluteModelPath: String) : Pointer {
            if (!File(absoluteModelPath).exists()) {
                throw IllegalArgumentException("Model file not found at: ${absoluteModelPath}")
            }

            println("LLM.init: Cache contains: ${sessionCache.keys}") ////
            println("LLM.init: Looking for: $absoluteModelPath") ////

            sessionCache[absoluteModelPath]?.let { session ->
                println("LlmExpert.init: Using existing session for $absoluteModelPath")
                return session
            }

            val session = conduitLib.conduit_llm_get_session(absoluteModelPath)
            sessionCache[absoluteModelPath] = session
                ?: throw RuntimeException("Failed to Load: ${absoluteModelPath}")

            println("LlmExpert.init: Successfully created session for $absoluteModelPath") ////
            return session
        }

        // ---------------------------------------------------------------------------------
        fun getResponse(sessionPtr: Pointer, prompt: String): Flow<String> = callbackFlow {
            val callback = object : ConduitTokenCallback {
                override fun invoke(text: String?, userData: Pointer?) {
                    trySend(text?: "")
                }
            }

            val rc = conduitLib.conduit_session_generate(sessionPtr, prompt, callback, null)
            when (rc) {
                0 -> close()
                1 -> close(CancellationException("Generation interrupted"))
                else -> close(RuntimeException("Generation failed (rc=$rc)"))
            }
        }

        fun abortResponse(expert: Expert) {
            conduitLib.conduit_session_abort_decode(expert.sessionPtr)
        }
    }
}
