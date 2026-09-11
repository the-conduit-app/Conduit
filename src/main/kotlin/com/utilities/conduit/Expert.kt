package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.Pointer
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.portals.ConduitPortal
import com.utilities.conduit.portals.EchoPortal
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

enum class ExpertType { INTERNAL, LLM, REMOTE }

@Serializable
class Expert(
    @Transient
    // ephemeral id
    val id: String = "E-${UUID.randomUUID().toString()}",

    val type: ExpertType,
    val nickname: String,
    val expertise: String,
    val model: String? = null,
    val description: String? = null,
    val color: String? = "Snazzy Slate",
) {
    // Runtime state shared by all Experts using the same model.
    var sessionPtr by mutableStateOf<Pointer?>(null)
    val seedPrompt: String
        get() = "You are: ${description ?: "a general expert"}"

    val isReady: Boolean
        get() = when (type) {
            ExpertType.LLM -> sessionPtr != null
            else -> true
        }

    // set from App() to a lambda that returns AppState.conduitUserModel
    internal var conduitUserModelProvider: () -> ConduitUserModel? = { null }
    internal fun setConduitUserModelGetter(provider: () -> ConduitUserModel?) {
        conduitUserModelProvider = provider
    }

    // For internal experts, ignore the chatThusFar context. They simply react algorithmically
    // to the prompt. For LLM experts we need to fetch the conduitUserModel, convert it to the
    // outwards facing user model, and send that.
    suspend fun getResponse(chatThusFar: String, prompt: String, includeUserModel: Boolean): Flow<String> {
        return when (type) {
            ExpertType.INTERNAL -> {
                when (model) {
                    ":conduit" -> ConduitPortal.getResponse(this, prompt)
                    else -> EchoPortal.getResponse(this, prompt)
                }
            }

            ExpertType.LLM -> {
                val sessionPtr = sessionPtr ?: error("Expert '${nickname}': LLM session not found.")
                val userModel = if (includeUserModel) {
                    val conduitUserModel = conduitUserModelProvider() // retrieves from AppState (in mem)
                    UserModel.convertToExternalUserModel(conduitUserModel)
                } else {
                    UserModel(
                        text = "NO USER MODEL IS REQUIRED FOR THIS TASK.",
                        lastSummaryModifiedTime = 0L
                    )
                }

                val finalPrompt = AppUtils.buildChatMlPrompt(
                    this.seedPrompt,
                    userModel.text,
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
                when (model) {
                    ":conduit" -> { ConduitPortal.abortResponse() }
                    else -> { EchoPortal.abortResponse() }
                }
            }

            ExpertType.LLM -> {
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
    const val SIMPLE_ECHO = ":simpleEcho"
    const val ROTTEN_ECHO = ":rottenEcho"
    const val SILLY_ECHO  = ":sillyEcho"
    const val SALAD_ECHO  = ":saladEcho"
    const val W_REV_ECHO  = ":ohceEcho"
    const val CONDUIT = ":conduit"
}
