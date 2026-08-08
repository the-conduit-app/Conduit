// Various Low priority maint jobs
// 1. runTitleMaintenance: Periodically scan all chats and rename potential candidates automatically
//
package com.utilities.conduit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

object Maintenance {
    val maintenanceMutex = Mutex()

    fun start(state: AppState, scope: CoroutineScope) {
        val chatsList = state.chatsList

        Trace.log("Maintenance start, Generating expert = ${state.chatManager.currentlyGeneratingExpert}")
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (state.chatManager.currentlyGeneratingExpert == null) {
                    Trace.log("Title maintenance run")
                    runTitleMaintenance(state, chatsList)
                }

                Trace.log("Maintenance 10s sleep")
                delay(10_000.milliseconds)
            }
        }
    }

    private suspend fun runTitleMaintenance(state: AppState, chatsList: ChatsList) {
        val systemExpert = state.systemExpert

        for (item in chatsList.items.toList()) {
            if (!item.chat.title.equals("Welcome to Conduit", ignoreCase = true))
                continue

            if (item.chat.nodes.size < 3)
                continue
            if (state.chatManager.currentlyGeneratingExpert != null)
                return

            Trace.log("Auto-renaming chat ${item.chat.title}")
            val newTitle = ChatUtils.generateChatTitle(systemExpert, item.chat)
            Trace.log("Suggested title: \"$newTitle\"")

            if (newTitle.equals(item.chat.title, ignoreCase = true))
                continue

            chatsList.rename(item, newTitle)
            chatsList.setNeedsHumanReview(item.chat.id, true)

            Trace.log("MaintTitle Yielding")
            yield()
        }
    }
}
