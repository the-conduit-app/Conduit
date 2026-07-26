package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sun.jna.Pointer
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

enum class ExpertType { INTERNAL, LOCAL, REMOTE }
enum class ExpertStatus { NONE, LOADING, READY, FAILED }

// -----------------------------------------------------------------------------
// Internal expert model identifiers

object InternalExperts {
    const val SYSTEM = ":system"

    const val SIMPLE_ECHO = ":simpleEcho"
    const val ROTTEN_ECHO = ":rottenEcho"
    const val SILLY_ECHO  = ":sillyEcho"
    const val SALAD_ECHO  = ":saladEcho"
    const val W_REV_ECHO  = ":wordReverseEcho"
}

// -----------------------------------------------------------------------------
// Expert

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
    // -------------------------------------------------------------------------
    // Runtime state (not serialized)

    @Transient
    private val handler: ExpertHandler = when (type) {
        ExpertType.INTERNAL -> InternalExpertHandler
        ExpertType.LOCAL    -> LlmExpertHandler
        ExpertType.REMOTE   -> throw NotImplementedError("Remote experts not yet supported")
    }

    @Transient
    var color: Color = Color.Gray          // Assigned by Pack.initialize()

    // NOTE: this sessionPtr is just a copy of the sessionPtr in LlmExpert.sessionCache
    // for convenience (to save repeated lookups). Hence it is updated for all "linked"
    // experts whenever an expert's status changes.
    @Transient
    var sessionPtr: Pointer? = null        // Assigned by LlmExpertHandler.initialize()

    // -------------------------------------------------------------------------
    // Observable state

    var status by mutableStateOf(ExpertStatus.NONE)

    // -------------------------------------------------------------------------

    companion object {

        fun setStatusOfGroup(
            state: AppState,
            modelPath: String,
            status: ExpertStatus,
            sessionPtr: Pointer? = null
        ) {
            state.expertsMap.values.forEach { expert ->
                if (expert.modelPath == modelPath) {
                    expert.status = status
                    if (sessionPtr != null) {
                        expert.sessionPtr = sessionPtr
                    }
                }
            }
        }
    }

    suspend fun initialize(state: AppState) {
        println("Expert (Init): $nickname")
        handler.initialize(this, state)
    }

    fun getResponse(state: AppState, messages: List<ChatMessage>): Flow<String> =
        handler.generateResponse(this, state, messages)
}
