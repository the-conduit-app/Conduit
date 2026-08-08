package com.utilities.conduit

import com.sun.jna.Pointer
import com.utilities.conduit.LlmPortal.conduitLib
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.lang.Thread.interrupted
import kotlin.coroutines.cancellation.CancellationException

object LlmPortal {
    private val conduitLibPath = AppUtils.getNativeLibPath("libconduit.dylib")
    val conduitLib: ConduitLib = com.sun.jna.Native.load(conduitLibPath, ConduitLib::class.java)

    fun createConduit(maxTokens: Long): Pointer = conduitLib.conduit_create(maxTokens)
            ?: error("Failed to create Conduit")
    fun destroyConduit(conduit: Pointer) { conduitLib.conduit_destroy(conduit) }

    fun freeSession(conduitPtr: Pointer, sessionPtr: Pointer) {
        conduitLib.conduit_destroy_session(conduitPtr, sessionPtr)
    }

    // ---------------------------------------------------------------------------------
    // Potentially time-consuming - Ensure it's called from an IO thread.
    fun initialize(conduitPtr: Pointer, absoluteModelPath: String): Pointer {
        if (!File(absoluteModelPath).exists()) {
            throw IllegalArgumentException("Model file not found at: $absoluteModelPath")
        }

        val sessionPtr = conduitLib.conduit_create_session(conduitPtr, absoluteModelPath)
            ?: throw RuntimeException("Failed to load model: $absoluteModelPath")

        return sessionPtr
    }

    // ---------------------------------------------------------------------------------
    fun getResponse(sessionPtr: Pointer, prompt: String): Flow<String> = callbackFlow {
        val callback = object : ConduitTokenCallback {
            override fun invoke(text: String?, userData: Pointer?) {
                trySend(text?: "")
            }
        }

        val rc = conduitLib.conduit_generate(sessionPtr, prompt, callback, null)
        when (rc) {
            ConduitLib.OK -> {
                close()
            }
            ConduitLib.OUTPUT_MAXED -> {
                trySend("\n\n[LARGE MESSAGE TRUNCATED]")
                close()
            }
            ConduitLib.ABORTED -> {
                close(CancellationException("Aborted by user"))
            }
            else -> {
                close(RuntimeException("Generation failed (rc=$rc)"))
            }
        }
    }

    fun abortResponse(sessionPtr: Pointer) {
        requireNotNull(sessionPtr) { "LlmPortal couldn't find a session to abort" }
        conduitLib.conduit_abort_generation(sessionPtr)
    }
}
