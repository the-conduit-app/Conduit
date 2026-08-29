package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.Pointer
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.EchoPortal
import com.utilities.conduit.portals.LlmPortal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

enum class ExpertType { INTERNAL, LOCAL, REMOTE }

@Serializable
class Expert(
    @Transient
    val id: String = UUID.randomUUID().toString(),

    val type: ExpertType,
    val nickname: String,
    val expertise: String,
    val modelPath: String? = null,
    val seedPrompt: String? = null,
) {
    // Runtime state shared by all Experts using the same model.
    var sessionPtr by mutableStateOf<Pointer?>(null)
    val isReady: Boolean
        get() = when (type) {
            ExpertType.LOCAL -> sessionPtr != null
            else -> true
        }

    fun getResponse(
        userModel: String?,
        chatThusFar: String,
        prompt: String
    ): Flow<String> {
        return when (type) {
            ExpertType.INTERNAL -> {
                EchoPortal.getResponse(this, prompt)
            }

            ExpertType.LOCAL -> {
                val sessionPtr = sessionPtr ?: error("Expert '${nickname}': LLM session not found.")
                val finalPrompt = AppUtils.buildChatMlPrompt(this.seedPrompt, userModel, chatThusFar, prompt)

                LlmPortal.getResponse(sessionPtr, finalPrompt)
            }

            else -> {
                flowOf("?")
            }
        }
    }

    fun abortResponse() {
        Trace.log("Expert Aborting Response: ${sessionPtr}")
        when (type) {
            ExpertType.INTERNAL -> {
                EchoPortal.abortResponse()
            }
            ExpertType.LOCAL -> {
                sessionPtr?.let {
                    LlmPortal.abortResponse(it)
                }
            }
            else -> {}
        }
    }
}

// Internal expert model Convenience identifiers
object InternalExperts {
    const val SYSTEM = ":system"

    const val SIMPLE_ECHO = ":simpleEcho"
    const val ROTTEN_ECHO = ":rottenEcho"
    const val SILLY_ECHO  = ":sillyEcho"
    const val SALAD_ECHO  = ":saladEcho"
    const val W_REV_ECHO  = ":wordReverseEcho"
}
