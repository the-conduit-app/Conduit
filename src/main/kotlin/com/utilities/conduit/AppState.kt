package com.utilities.conduit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.sun.jna.Pointer
import com.utilities.conduit.chat.ChatManager
import com.utilities.conduit.chat.ChatsList
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.maintenance.Maintenance
import com.utilities.conduit.packs.Pack
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.ui.LeftPanelMode
import com.utilities.conduit.ui.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppState(
    val conduitPtr: Pointer,
    val scope: CoroutineScope,
    val currentExpert: MutableState<Expert?>,
    val currentPack: MutableState<Pack?>,
    val expertsMap: SnapshotStateMap<String, Expert>,
    val systemExpert: Expert,
    val chatManager: ChatManager
) {
    // Only LLMs approved by Conduit can be loaded to reduce native crash poss
    // Power users can edit APPDIR/.approved-models.json
    var approvedModels: Map<String, ApprovedModel> = emptyMap()
    var chatsList by mutableStateOf(ChatsList())
    var availablePacks by mutableStateOf(emptyList<Pack>())
    val notification = Notification(scope)
    internal var conduitUserModel by mutableStateOf<ConduitUserModel?>(null)

    var leftPanelMode by mutableStateOf(LeftPanelMode.LIST)
    val focusInput = mutableStateOf(0)

    var lastUserActivity = System.currentTimeMillis()

    companion object {
        fun createSystemExpert() = Expert(
            type = ExpertType.LLM,
            nickname = "Conduit",
            expertise = "General",
            model = "gemma-2-9b-it-Q4_K_M.gguf",
            description = "A general purpose expert assisting with various administrative tasks " +
                    "like summarizing chats, generating titles, the user model, etc.",
            color = "Mr. Slater"
        )

        // One new AppState per invocation
        fun createNew(conduitPtr: Pointer, scope: CoroutineScope): AppState {
            val systemExpert = createSystemExpert()
            val state = AppState(
                conduitPtr = conduitPtr,
                scope = scope,
                chatManager = ChatManager(),

                currentExpert = mutableStateOf(null),
                currentPack = mutableStateOf(null),
                expertsMap = mutableStateMapOf(),
                systemExpert = systemExpert
            )

            return state
        }
    }

    suspend fun shutdown() = withContext(Dispatchers.IO) {
        Trace.log("APP: SHUTDOWN BEGIN")

        Maintenance.stop()
        Trace.log("APP: MAINT STOPPED")

        chatManager.stopGeneration()
        Trace.log("APP: GENERATION STOPPED")

        Trace.log("APP: DESTROYING CONDUIT")
        LlmPortal.conduitLib.conduit_destroy(conduitPtr)
        Trace.log("APP: CONDUIT DESTROYED")
    }
}
