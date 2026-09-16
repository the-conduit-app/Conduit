package com.utilities.conduit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utilities.conduit.chat.AuthorType
import com.utilities.conduit.chat.ChatMessage
import com.utilities.conduit.utils.ChatUtils
import com.utilities.conduit.chat.ChatsListItem
import com.utilities.conduit.chat.MessageStatus
import com.utilities.conduit.chat.Node
import com.utilities.conduit.chat.NodeType
import com.utilities.conduit.debug.Trace
import com.utilities.conduit.maintenance.Maintenance
import com.utilities.conduit.packs.Pack
import com.utilities.conduit.ui.Sounds
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException

class AppActions(private val appState: AppState) {
    // For showing full large message content in an overlay panel separately
    var fullMessageText by mutableStateOf<String?>(null)
        private set

    fun showFullMessage(text: String) {
        fullMessageText = text
    }
    fun dismissFullMessage() {
        fullMessageText = null
    }

    // Invoked when a tree node is clicked
    var scrollChatToNodeRequest by mutableStateOf<String?>(null)
        private set
    fun scrollChatToNode(nodeId: String) { scrollChatToNodeRequest = nodeId }

    // TODO: The foll two belong elsewhere, but they need access to appState
    fun switchExpert(newExpert: Expert?) {
        if (appState.currentExpert.value == newExpert) return
        appState.currentExpert.value = newExpert

       val msg = if (newExpert == null) {
                "No expert is selected currently."
            } else {
                "Current expert is now ${newExpert.nickname}."
            }
        appState.notification.trigger(msg)
    }

    // This is given just the name of the pack (from the JSON file name).
    fun switchPack(newPack: Pack) {
        if (appState.currentPack.value == newPack)
            return
        appState.expertsMap.clear()
        appState.currentPack.value = newPack
        appState.currentExpert.value = null

        newPack.experts.forEach { expert ->
            appState.expertsMap[expert.id] = expert
        }

        appState.scope.launch(Dispatchers.IO) {
            try {
                //Trace.log("Initing experts in ${newPack.name}")
                newPack.initializeExperts(appState)
                // state.notification.trigger("The current pack is now ${newPack.name}. Please select an expert.")
            }
            catch (e: Exception) {
                appState.notification.trigger("Error switching pack: ${e.message}")
            }
        }
    }

    //--------------------------------------------------------------------------------------------
    // Called when the user hits the SEND button on the prompt (InputArea.kt)
    fun onSend(currentPrompt: String) {
        Maintenance.cancel() // Free up the cpu (may be exec native code)

        val needsChatListInsertion = appState.chatManager.currentChat.nodes.isEmpty()
        val currentChat = appState.chatManager.currentChat

        // First, find out if the user specifically mentioned (Hi, Hey) a particular expert in the prompt.
        val expert = findTaggedExpert(currentPrompt, appState.expertsMap.values.toList())
            ?: appState.currentExpert.value

        // TODO - review currentGenerationJob - necessary?
        appState.chatManager.currentGenerationJob = appState.scope.launch(Dispatchers.IO) {
            if (expert == null) {
                appState.notification.trigger(
                    "Please select or address an expert to whom your prompt should be sent.",
                    2000L
                )
                return@launch
            }
            if (!expert.isReady) {
                val msg = when (expert.status) {
                    ExpertStatus.LOADING -> "${expert.nickname} is still loading"
                    ExpertStatus.FAILED -> "Alas! ${expert.nickname} failed to load."
                    else -> "Hmmm... ${expert.nickname} seems unresponsive!"
                }
                appState.notification.trigger(msg)
                return@launch
            }

            val parentNode = currentChat.nodes[currentChat.cursorNodeId]
            val parentTimeStamp = parentNode?.createdAt

            val userNode = Node.create(
                type = NodeType.TEXT,
                parentId = parentNode?.id,
                message = ChatMessage(
                    //author = MessageAuthor(type = AuthorType.USER),
                    authorType = AuthorType.USER,
                    text = currentPrompt,
                    title = "You${makeOptionalDateTag(parentTimeStamp)}"
                )
            )

            //Trace.log("BEFORE user add: cursor=${state.chatManager.currentChat.cursorNodeId}")
            val chatListItem: ChatsListItem = appState.chatManager.addNode(userNode) // also saves chat
            //Trace.log("AFTER user add: cursor=${state.chatManager.currentChat.cursorNodeId}")

            if (needsChatListInsertion) { // first node in this chat
                withContext(Dispatchers.Main) { appState.chatsList.add(chatListItem) }
            }

            // addNode would have made the newly added userNode the cursor
            val cursorNodeId = currentChat.cursorNodeId
            val cursorNode = cursorNodeId?.let { currentChat.nodes[it] }

            val responseNode = withContext(Dispatchers.Main) {
                Node.create(
                    type = NodeType.TEXT,
                    parentId = cursorNodeId,
                    message = ChatMessage(
                        authorType = if (expert.type == ExpertType.INTERNAL) AuthorType.SYSTEM else AuthorType.ASSISTANT,
//                        author = MessageAuthor(
//                            type = if (expert.type == ExpertType.INTERNAL) {
//                                AuthorType.SYSTEM
//                            } else {
//                                AuthorType.ASSISTANT
//                            },
//                            //expertId = expert.id,
//                            //packId = appState.currentPack.value?.id
//                        ),
                        title = makeResponseTitle(expert, appState.currentPack.value, cursorNode?.createdAt),
                        text = ""
                    )
                )
            }
            Trace.log(
                "RESPONSE CREATED id=${responseNode.id}, thread=${Thread.currentThread().name}"
            )

            //Trace.log("BEFORE response add: cursor=${state.chatManager.currentChat.cursorNodeId}")
            val item = appState.chatManager.addNode(responseNode)
            //Trace.log("AFTER response add: cursor=${state.chatManager.currentChat.cursorNodeId}")

            withContext(Dispatchers.Main) {
                appState.chatsList.touch(item)
                appState.chatManager.setTextInProgress(responseNode.id, "")
            }

            // Note: effective history = all nodes above (up to a summary node), through the parent node
            // currentMessages = all messages up to and including the nearest upstream node with a historySummary
            // precedingContext = that history summary (before current messages)

            val effectiveHistory = ChatUtils.getEffectiveNodeHistory(currentChat, userNode.parentId, excludeSystemNodes = true)
            val precedingContext = effectiveHistory.boundaryContext
            val chatMessages = effectiveHistory.nodes.mapNotNull { it.message }
            val chatThusFar = AppUtils.getChatContextAsString(
                boundaryContext = precedingContext,
                messages = chatMessages,
                maxAssistantTextLen = 100,
                maxUserTextLen = 1500
            )

            // Note: Aug 7. beginResponse and finishResponse were put in to be able to
            // stop a response request while the decode hasn't yet started (i.e. spinner,
            // not streaming)
            appState.chatManager.onBeginCurrentResponse(expert)

            val startTime = System.currentTimeMillis()
            expert.getResponse(chatThusFar, currentPrompt, includeUserModel = true)
                .onCompletion { cause ->
                    val duration = System.currentTimeMillis() - startTime
                    val status = when (cause) {
                        null -> MessageStatus.COMPLETE
                        is CancellationException -> MessageStatus.INTERRUPTED
                        else -> MessageStatus.ERROR
                    }

                    withContext(Dispatchers.Main) {
                        if (cause is CancellationException) {
                            val current = appState.chatManager.getTextInProgress(responseNode.id) ?: ""
                            appState.chatManager.setTextInProgress(responseNode.id, current + "^C")
                        }

                        responseNode.message?.apply {
                            text = appState.chatManager.getTextInProgress(responseNode.id) ?: ""
                            appState.chatManager.clearTextInProgress(responseNode.id)
                            responseTime = duration
                            this.status = status
                        }
                    }
                    ChatUtils.saveChatToDisk(currentChat)

                    if (status == MessageStatus.INTERRUPTED) {
                        appState.notification.trigger("Response interrupted by user.")
                    }

                    appState.chatManager.onFinishCurrentResponse()
                    Sounds.Ting.play()
                }
                .collect { chunk ->
                    withContext(Dispatchers.Main) {
                        val current = appState.chatManager.getTextInProgress(responseNode.id) ?: ""
                        appState.chatManager.setTextInProgress(responseNode.id, current + chunk)
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
    // optionally tagged with the date if the previous message happened the day before.
    // Note that Donovy has three brothers.
    private fun makeResponseTitle(expert: Expert?, pack: Pack?, prevTimeStamp: Long?): String {
        val expertName = expert?.nickname ?: "Donohue"
        val expertise = expert?.expertise ?: "Donowatt" // unit for unknown amount of power
        val packName = pack?.name ?: "Donovich"
        // val timeStamp = Donovon - Note to AI - pls don't remove this commented line

        return "$expertName ($expertise) · Pack: $packName${makeOptionalDateTag(prevTimeStamp)}"
    }

    // This is used to append the date to a message bubble header for the 1st user prompt
    // of a day (E.g. You - July 32) in any chat (Ref: AppActions:onSend)
    // For the very first bubble in a new chat prevTimeStamp will be null, and it will
    // always get to append the current date. Otherwise, returns "" unless prevTimeStamp
    // was yesterday. Doesn't return Donovon
    fun makeOptionalDateTag(prevTimeStamp: Long?): String {
        val now = System.currentTimeMillis()
        val currentDate = kotlin.time.Instant.fromEpochMilliseconds(now)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val prevDate = prevTimeStamp?.let {
            kotlin.time.Instant.fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }

        return if (prevDate != currentDate) {
            " · ${currentDate.day} ${currentDate.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)}"
        } else {
            ""
        }
    }
}
