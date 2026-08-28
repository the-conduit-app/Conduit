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
    var chatsList by mutableStateOf(ChatsList())
    var availablePacks by mutableStateOf(emptyList<Pack>())
    val notification = Notification(scope)

//    val rightScreenCurtain = ScreenCurtain(scope)
//    val leftScreenCurtain = ScreenCurtain(scope)

    var leftPanelMode by mutableStateOf(LeftPanelMode.LIST)
    val focusInput = mutableStateOf(0)

    var lastUserActivity = System.currentTimeMillis()

    companion object {
        fun createSystemExpert() = Expert(
            id = "SYSTEM.ID",
            type = ExpertType.LOCAL,
            nickname = "Conduit",
            expertise = "General",
            modelPath = "llm/gemma-2-9b-it-Q4_K_M.gguf",
            seedPrompt = "You are a general purpose expert. You assist with various administrative tasks " +
                    "like summarizing chats, generating titles, the user model, etc."
        )

        // One new AppState per invocation
        fun createNew(conduitPtr: Pointer, scope: CoroutineScope): AppState {
            val systemExpert = createSystemExpert()
            val state = AppState(
                conduitPtr = conduitPtr,
                scope = scope,
                chatManager = ChatManager(scope, systemExpert),
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
