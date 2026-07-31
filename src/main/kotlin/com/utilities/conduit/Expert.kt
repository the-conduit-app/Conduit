package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.Pointer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

enum class ExpertType { INTERNAL, LOCAL, REMOTE }
enum class ExpertStatus { NONE, LOADING, READY, FAILED }

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
    @Transient
    private val handler: ExpertHandler = when (type) {
        ExpertType.INTERNAL -> InternalExpertHandler
        ExpertType.LOCAL    -> LocalExpertHandler
        ExpertType.REMOTE   -> throw NotImplementedError("Remote experts not yet supported")
    }

    // NOTE: this sessionPtr is just a copy of the sessionPtr in LlmExpert.sessionCache
    // for convenience (to save repeated lookups). Hence, it is updated for all "linked"
    // experts whenever an expert's status changes. It is a pointer to the conduit c++
    // library gateway
    @Transient
    var sessionPtr: Pointer? = null        // Assigned by LlmExpertHandler.initialize()

    // Observable status
    var status by mutableStateOf(ExpertStatus.NONE)

    suspend fun initialize(state: AppState) {
        handler.initialize(this, state)
    }

    fun getResponse(state: AppState, messages: List<ChatMessage>): Flow<String> =
        handler.generateResponse(this, state, messages)

    // Called from AppActions:onSend:.collect() during response generation
    fun abortResponse() {
        handler.abortResponse(this)
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
