package com.utilities.conduit

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.Callback

interface ConduitTokenCallback : Callback {
    fun invoke(piece: String?, userData: Pointer?)
}

interface ConduitLib : Library {
    fun conduit_llm_init()
    fun conduit_llm_free()
    fun conduit_llm_get_session(absoluteModelPath: String) : Pointer?
    fun conduit_llm_free_session(sessionPtr: Pointer?)
    fun conduit_session_generate(sessionPtr: Pointer?, prompt: String,
                                 callback: ConduitTokenCallback, userData: Pointer?): Int
}
