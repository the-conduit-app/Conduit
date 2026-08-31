package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sun.jna.Pointer
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.EchoPortal
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
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
    val description: String? = null,
    val color: String? = "Snazzy Slate",
) {
    // Runtime state shared by all Experts using the same model.
    var sessionPtr by mutableStateOf<Pointer?>(null)
    val seedPrompt: String
        get() = "You are: ${description ?: "a general expert"}"

    val isReady: Boolean
        get() = when (type) {
            ExpertType.LOCAL -> sessionPtr != null
            else -> true
        }

    fun getResponse(userModel: String?, chatThusFar: String, prompt: String): Flow<String> {
        return when (type) {
            ExpertType.INTERNAL -> { EchoPortal.getResponse(this, prompt) }

            ExpertType.LOCAL -> {
                val sessionPtr = sessionPtr ?: error("Expert '${nickname}': LLM session not found.")
                val finalPrompt = AppUtils.buildChatMlPrompt(
                    this.seedPrompt,
                    userModel ?: "NO EXISTING USER MODEL",
                    chatThusFar,
                    prompt
                )
                Trace.log("EXPERT.GENERATE final prompt = $finalPrompt")

                LlmPortal.getResponse(sessionPtr, finalPrompt)
            }

            else -> { flowOf("?") }
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


val expertColors = mapOf(
    "Proud Peacock" to Color(0xFF007C91),
    "Misty Blue" to Color(0xFF8DB9CC),
    "Dusty Rose" to Color(0xFFD09AAA),
    "Muted Gold" to Color(0xFFD5BF72),
    "Cutey Peach" to Color(0xFFD5A084),
    "Surprisingly Sage" to Color(0xFF91B99A),
    "Snazzy Slate" to Color(0xFF9EADB3),
    "Limpid Lavender" to Color(0xFFB3A6C7),
    "Terracotta" to Color(0xFFC58B78),
    "Slippery Seafoam" to Color(0xFF8FB9AD),
    "Pretty Periwinkle" to Color(0xFF9FAED0),
    "Arisi Mauve" to Color(0xFFB596A8)
)
