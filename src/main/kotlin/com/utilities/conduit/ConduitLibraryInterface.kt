package com.utilities.conduit

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Pointer

interface ConduitTokenCallback : Callback {
    fun invoke(text: String?, userData: Pointer?)
}

interface ConduitLib : Library {
    companion object {
        const val OK = 0
        const val ABORTED = 1

        const val OUTPUT_MAXED = -987
    }
    fun conduit_create(maxGenTokens: Long): Pointer?
    fun conduit_destroy(conduitPtr: Pointer?)

    fun conduit_create_session(conduitPtr: Pointer?, absoluteModelPath: String): Pointer?
    fun conduit_destroy_session(conduitPtr: Pointer?, sessionPtr: Pointer?)
    fun conduit_generate(sessionPtr: Pointer?, prompt: String, callback: ConduitTokenCallback, userData: Pointer?): Int

    fun conduit_abort_generation(sessionPtr: Pointer?)
}
