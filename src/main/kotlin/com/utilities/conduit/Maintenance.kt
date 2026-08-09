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

        scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (state.chatManager.currentlyGeneratingExpert == null) {
                    runTitleMaintenance(state, chatsList)
                }
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

            val newTitle = ChatUtils.generateChatTitle(systemExpert, item.chat)
            if (newTitle.equals(item.chat.title, ignoreCase = true))
                continue

            chatsList.rename(item, newTitle)
            chatsList.setNeedsHumanReview(item.chat.id, true)

            yield()
        }
    }
}
