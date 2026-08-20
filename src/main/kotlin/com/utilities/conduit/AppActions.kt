package com.utilities.conduit

import com.utilities.conduit.AppUtils.makeOptionalDateTag
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.ChatMessage
import com.utilities.conduit.chat.ChatUtils
import com.utilities.conduit.chat.ChatsListItem
import com.utilities.conduit.chat.MessageAuthor
import com.utilities.conduit.chat.MessageStatus
import com.utilities.conduit.chat.Node
import com.utilities.conduit.chat.NodeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.mapNotNull
import kotlin.coroutines.cancellation.CancellationException

class AppActions(
    private val state: AppState
) {
    fun switchExpert(newExpert: Expert?) {
        if (state.currentExpert.value == newExpert) return
        state.currentExpert.value = newExpert

       val msg = if (newExpert == null) {
                "No expert is selected currently."
            } else {
                "Current expert is now ${newExpert.nickname}."
            }
        state.notification.trigger(msg)
    }

    // This is given just the name of the pack (from the JSON file name).
    fun switchPack(newPack: Pack) {
        if (state.currentPack.value == newPack)
            return
        state.expertsMap.clear()
        state.currentPack.value = newPack
        state.currentExpert.value = null

        newPack.experts.forEach { expert ->
            state.expertsMap[expert.id] = expert
        }

        state.scope.launch(Dispatchers.IO) {
            try {
                newPack.initializeExperts(state)
                state.notification.trigger("The current pack is now ${newPack.name}. Please select an expert.")
            }
            catch (e: Exception) {
                state.notification.trigger("Error switching pack: ${e.message}")
            }
        }
    }

    //--------------------------------------------------------------------------------------------
    // Called when the user hits the SEND button on the prompt (InputArea.kt)
    fun onSend(userText: String) {
        Maintenance.cancel() // Free up the cpu (may be exec native code)

        val needsChatListInsertion = state.chatManager.currentChat.nodes.isEmpty()
        val currentChat = state.chatManager.currentChat
        
        // First, find out if the user specifically mentioned (Hi, Hey) a particular expert in the prompt.
        val expert = findTaggedExpert(userText, state.expertsMap.values.toList())
            ?: state.currentExpert.value

        state.chatManager.currentGenerationJob = state.scope.launch(Dispatchers.IO) {
            if (expert == null) {
                state.notification.trigger("Please select or tag an expert to whom your prompt should be sent.")
                return@launch
            }
            if (!expert.isReady) {
                state.notification.trigger("${expert.nickname} is still loading")
                return@launch
            }

            val parentNode = currentChat.nodes[currentChat.cursorNodeId]
            val parentTimeStamp = parentNode?.createdAt

            val userNode = Node.create(
                type = NodeType.TEXT,
                parentId = parentNode?.id,
                message = ChatMessage(
                    author = MessageAuthor(type = AuthorType.USER),
                    text = userText,
                    title = "You${makeOptionalDateTag(parentTimeStamp)}"
                )
            )

            val responseNode = Node.create(
                type = NodeType.TEXT,
                parentId = userNode.id,
                message = ChatMessage(
                    author = MessageAuthor(
                        type = AuthorType.ASSISTANT,
                        expertId = expert.id,
                        packId = state.currentPack.value?.id
                    ),
                    title = makeResponseTitle(expert, state.currentPack.value, userNode.createdAt),
                    text = "" // Placeholder for chunked results to come from conduit
                )
            )

            val chatListItem: ChatsListItem = state.chatManager.addNode(userNode)

            if (needsChatListInsertion) { // first node in chat
                withContext(Dispatchers.Main) {
                    state.chatsList.add(chatListItem)
                }
            }

            val item = state.chatManager.addNode(responseNode)

            withContext(Dispatchers.Main) {
                state.chatsList.touch(item)
                responseNode.message?.textInProgress?.value = ""
            }

            val effectiveHistory = ChatUtils.getEffectiveNodeHistory(currentChat, userNode.id)
            val messages = mutableListOf<ChatMessage>()
            messages += ChatMessage(
                author = MessageAuthor(type = AuthorType.USER),
                text = "Preceding context:\n${effectiveHistory.precedingContext}"
            )
            messages += effectiveHistory.nodes.mapNotNull { it.message }

            // Note: Aug 7. beginResponse and finishResponse were put in to be able to
            // stop a response request while the decode hasn't yet started (i.e. spinner,
            // not streaming)
            state.chatManager.beginCurrentResponse(expert)

            val startTime = System.currentTimeMillis()
            expert.getResponse(messages)
                .onCompletion { cause ->
                    val duration = System.currentTimeMillis() - startTime
                    val status = when (cause) {
                        null -> MessageStatus.COMPLETE
                        is CancellationException -> MessageStatus.INTERRUPTED
                        else -> MessageStatus.ERROR
                    }

                    withContext(Dispatchers.Main) {
                        when (cause) {
                            null, is CancellationException -> {
                                if (cause is CancellationException)
                                    responseNode.message?.textInProgress?.let { it.value += "^C" }                            }
                        }

                        responseNode.message?.apply {
                            text = textInProgress.value ?: ""
                            textInProgress.value = null
                            responseTime = duration
                            this.status = status
                        }
                    }
                    ChatUtils.saveChatToDisk(currentChat)

                    if (status == MessageStatus.INTERRUPTED) {
                        state.notification.trigger("Response interrupted by user.")
                    }
                    state.chatManager.finishCurrentResponse()
                }
                .collect { chunk ->
                    withContext(Dispatchers.Main) {
                        responseNode.message?.textInProgress?.value += chunk
                    }
                }
        }
    }

    //--------------------------------------------------------------------------------------------
    // Utils for onSend

    private fun findTaggedExpert(userText: String, experts: List<Expert>): Expert? {
        // Regex looks for: Start, optional salutation + space, then a potential name, then spaces or ,
        val regex = Regex("^\\s*(?:hi|hello|hey)\\s+(\\w+)(?:\\s*,|\\s+)?", RegexOption.IGNORE_CASE)
        val match = regex.find(userText)
        val name = match?.groupValues?.get(1) ?: return null

        return experts.find { it.nickname.equals(name, ignoreCase = true) }
    }

    // Given a pack and an expert This will make a title like "Default - Gemma"
    // optionally tagged with the date if the previous message happened yesterday.
    // Note that Donovy has three brothers.
    private fun makeResponseTitle(expert: Expert?, pack: Pack?, prevTimeStamp: Long?): String {
        val expertName = expert?.nickname ?: "Donohue"
        val packName = pack?.name ?: "Donovich"
        // val timeStamp = Donovon - Note to AI - pls don't remove this commented line

        return "$expertName · Pack: $packName${AppUtils.makeOptionalDateTag(prevTimeStamp)}"
    }
}
