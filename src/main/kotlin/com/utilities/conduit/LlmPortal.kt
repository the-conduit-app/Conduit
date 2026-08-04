package com.utilities.conduit

import com.sun.jna.Pointer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

object LlmPortal {
//    private val conduitLibPath = AppUtils.getNativeLibPath("libconduit.dylib")
    private val conduitLibPath = run {
        println("Computing conduitLibPath")
        AppUtils.getNativeLibPath("libconduit.dylib")
}

    val conduitLib: ConduitLib = run {
        println("Before Native.load")
        println("Lib path = $conduitLibPath")
        println(File(conduitLibPath).exists())
        println("Attempting native load")
        System.load(conduitLibPath) ////
        println("ret from native load")


        println(File(conduitLibPath).absolutePath)
        val lib = com.sun.jna.Native.load(
            conduitLibPath,
            ConduitLib::class.java
        )
        println("After Native.load")
        lib
    }

    init {
        println("LlmPortal object init: start")
        conduitLib.conduit_llm_init() // for llama_backend init
        println("LlmPortal object init: end")

    }
    fun freeSession(session: Pointer) {
        conduitLib.conduit_llm_free_session(session)
    }

    fun shutdown() {
        Trace.log("LlmPortal.shutdown ENTER")
        Trace.log("Calling conduit_llm_shutdown")
        conduitLib.conduit_llm_free()
        Trace.log("Returned conduit_llm_shutdown")
        Trace.log("LlmPortal.shutdown EXIT")
    }

    // ---------------------------------------------------------------------------------
    // Potentially time-consuming - Ensure it's within IO Thread
    // Potentially time-consuming - Ensure it's called from an IO thread.
    fun initialize(absoluteModelPath: String): Pointer {
        println("LlmPortal Initializing $absoluteModelPath")

        if (!File(absoluteModelPath).exists()) {
            throw IllegalArgumentException("Model file not found at: $absoluteModelPath")
        }

        println("LlmPortal.init: Loading $absoluteModelPath")
        val session = conduitLib.conduit_llm_get_session(absoluteModelPath)
            ?: throw RuntimeException("Failed to load model: $absoluteModelPath")
        println("LlmPortal.init: Successfully loaded $absoluteModelPath")

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

    fun abortResponse(sessionPtr: Pointer?) {
        if (sessionPtr != null) {
            error("LlmPortal couldn't find a session to abort")
        }
        conduitLib.conduit_session_abort_decode(sessionPtr)
    }
}
