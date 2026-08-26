package com.utilities.conduit.portals

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.utilities.conduit.AppUtils
import com.utilities.conduit.debug.Trace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

object LlmPortal {
    private val conduitLibPath = AppUtils.getNativeLibDir("libconduit.dylib")
    val conduitLib: ConduitLib = Native.load(conduitLibPath, ConduitLib::class.java)

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
        //Trace.log("Sending prompt: $prompt")
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
                //Trace.log("LLM: conduit_generate ABORTED — closing Flow")
                close(CancellationException("Aborted by user"))
            }
            else -> {
                close(RuntimeException("Generation failed (rc=$rc)"))
            }
        }
    }

    fun abortResponse(sessionPtr: Pointer) {
        requireNotNull(sessionPtr) { "LlmPortal couldn't find a session to abort" }
        Trace.log("Aborting response: $sessionPtr")

        conduitLib.conduit_abort_generation(sessionPtr)
    }
}
