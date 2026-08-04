package com.utilities.conduit

import com.sun.jna.Pointer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.lang.Thread.interrupted
import kotlin.coroutines.cancellation.CancellationException

object LlmPortal {
    private val conduitLibPath = run {
        AppUtils.getNativeLibPath("libconduit.dylib")
    }

    val conduitLib: ConduitLib = run {
        com.sun.jna.Native.load(conduitLibPath, ConduitLib::class.java)
    }

    init {
        conduitLib.conduit_llm_init() // for llama_backend init

    }
    fun freeSession(session: Pointer) {
        conduitLib.conduit_llm_free_session(session)
    }

    fun shutdown() {
        conduitLib.conduit_llm_free()
    }

    // ---------------------------------------------------------------------------------
    // Potentially time-consuming - Ensure it's within IO Thread
    // Potentially time-consuming - Ensure it's called from an IO thread.
    fun initialize(absoluteModelPath: String): Pointer {
        if (!File(absoluteModelPath).exists()) {
            throw IllegalArgumentException("Model file not found at: $absoluteModelPath")
        }

        val session = conduitLib.conduit_llm_get_session(absoluteModelPath)
            ?: throw RuntimeException("Failed to load model: $absoluteModelPath")

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
            0 -> { Trace.log("normal close"); close() }
            1 -> { Trace.log("cancelled flow close"); close(CancellationException("Generation interrupted")) }
            else -> { Trace.log("error flow rc = $rc"); close(RuntimeException("Generation failed (rc=$rc)")) }
        }
    }

    fun abortResponse(sessionPtr: Pointer?) {
        requireNotNull(sessionPtr) { "LlmPortal couldn't find a session to abort" }
        conduitLib.conduit_session_abort_decode(sessionPtr)
    }
}
