package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.Pointer
import kotlinx.coroutines.flow.Flow
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
    @Transient
    private val handler: ExpertHandler = when (type) {
        ExpertType.INTERNAL -> InternalExpertHandler
        ExpertType.LOCAL    -> LocalExpertHandler
        ExpertType.REMOTE   -> throw NotImplementedError("Remote experts not yet supported")
    }

    // Runtime state shared by all Experts using the same model.
    var sessionPtr by mutableStateOf<Pointer?>(null)
    val isReady: Boolean
        get() = when (type) {
            ExpertType.LOCAL -> sessionPtr != null
            else -> true
        }


    fun getResponse(messages: List<ChatMessage>): Flow<String> =
        handler.generateResponse(this, messages)

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
