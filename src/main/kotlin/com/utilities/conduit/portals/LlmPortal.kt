package com.utilities.conduit.portals

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    // Returns null if native create session fails.
    fun initialize(conduitPtr: Pointer, absoluteModelPath: String, modelSha: String): Pointer? {
        return conduitLib.conduit_create_session(conduitPtr, absoluteModelPath, modelSha)
    }

    // ---------------------------------------------------------------------------------
    fun getResponse(sessionPtr: Pointer, prompt: String): Flow<String> = callbackFlow {
        //Trace.log("Sending prompt: $prompt")
        val callback = object : ConduitTokenCallback {
            override fun invoke(text: String?, userData: Pointer?) {
                val result = trySendBlocking(text ?: "")
                if (result.isFailure) {
                    Trace.log("LLM CALLBACK: trySendBlocking FAILED text=[$text]")
                }
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
        Trace.log("conduit_generate returned $rc")
    }

    fun abortResponse(sessionPtr: Pointer) {
        requireNotNull(sessionPtr) { "LlmPortal couldn't find a session to abort" }
        Trace.log("Aborting response: $sessionPtr")

        conduitLib.conduit_abort_generation(sessionPtr)
    }
}
