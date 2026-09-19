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
enum class ExpertStatus { LOADING, READY, FAILED }

// 1. A Conduit Pack contains a bunch of experts (See a sample pack.json)
// 2. the description is converted into the pre-fill seed prompt when the expert is used to generate
//    (e.g. description: "All-round expert" will become "You are an all-round expert")
// 3. nickname and expertise are used only in the UI (icons and chat message bubbles)
// 4. color is simply to give the experts distinct icons in the top panel
@Serializable
class Expert(
    // We don't actually serialize Experts; so the id is just internal and ephemeral to
    // a particular Conduit session.
    @Transient
    val id: String = "E-${UUID.randomUUID().toString()}",

    val type: ExpertType,
    val nickname: String,
    val expertise: String,
    val model: String? = null,
    val description: String? = null,
    val color: String? = "Snazzy Slate",
) {
    // Runtime state shared by all Experts using the same model. But the native side
    // actually clears context before each generation and supplies context extracted
    // from the current chat itself.
    var sessionPtr by mutableStateOf<Pointer?>(null)
    val seedPrompt: String
        get() = "You are: ${description ?: "a general expert"}"

    // status is ONLY used for differentially rendering the ExpertIcon
    // For all other uses, isReady is the state to check.
    var status: ExpertStatus by mutableStateOf(ExpertStatus.LOADING)
    val isReady: Boolean
        get() = when (type) {
            ExpertType.LLM -> sessionPtr != null
            else -> true
        }

    // set from App() to a lambda that returns AppState.conduitUserModel. We use this
    // to fetch the user model to pre-fill in case a generation task requires it (getResponse below)
    internal var conduitUserModelProvider: () -> ConduitUserModel? = { null }
    internal fun setConduitUserModelGetter(provider: () -> ConduitUserModel?) {
        conduitUserModelProvider = provider
    }

    // For internal experts, ignore the chatThusFar context. They simply react algorithmically
    // to the prompt. For LLM experts we need to fetch the conduitUserModel, convert it to the
    // outwards facing user model, and send that.
    suspend fun getResponse(chatThusFar: String, prompt: String, includeUserModel: Boolean): Flow<String> {
        return when (type) {
            // Echoes and Condy are the only internal experts
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
                    this.nickname,
                    this.seedPrompt,
                    userModel.text,
                    chatThusFar,
                    prompt
                )
                // Uncomment to tune prompt
                // Trace.log("EXPERT.GENERATE final prompt = $finalPrompt")

                LlmPortal.getResponse(sessionPtr, finalPrompt)
            }

            else -> { flowOf("?") }
        }
    }

    fun abortResponse() {
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

// Internal expert models - convenience identifiers
object InternalExperts {
    const val SIMPLE_ECHO = ":simpleEcho"
    const val ROTTEN_ECHO = ":rottenEcho"
    const val SILLY_ECHO  = ":sillyEcho"
    const val SALAD_ECHO  = ":saladEcho"
    const val W_REV_ECHO  = ":ohceEcho"
    const val CONDUIT = ":conduit"
}
