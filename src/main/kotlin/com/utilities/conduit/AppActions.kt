package com.utilities.conduit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class AppActions(
    private val scope: CoroutineScope,
    private val state: AppState
) {
    fun notify(message: String) {
        state.notification.value = message
    }

    fun switchExpert(newExpert: Expert?) {
        if (state.currentExpert.value == newExpert) return
        state.currentExpert.value = newExpert

        notify(
            if (newExpert == null) {
                "No expert is selected currently."
            } else {
                "Current expert is now ${newExpert.nickname}."
            }
        )
    }

    // Simultaneously initialize ALL experts sharing the same LLM model file path
    fun retryExpertInitialization(expert: Expert) {
        // Never purge the system expert
        if (expert.modelPath == null || expert.modelPath == state.systemExpert?.modelPath)
            return

        scope.launch {
            state.setStatusOfExpertGroup(expert.modelPath, ExpertStatus.LOADING)
            state.expertsMap.values
                .filter { it.modelPath == expert.modelPath }
                .forEach { targetExpert ->
                    scope.launch(Dispatchers.IO) { targetExpert.initialize(state) }
                }
        }
    }

    // This is given just the name of the pack (from the JSON file name).
    fun switchPack(newPack: Pack) {
        if (state.currentPack.value == newPack)
            return
        state.expertsMap.clear()
        state.currentPack.value = newPack
        state.currentExpert.value = null

        scope.launch(Dispatchers.IO) {
            try {
                newPack.initialize(state, scope)
                notify("The current pack is now ${newPack.name}. Please select an expert.")
            }
            catch (e: Exception) {
                notify("Error switching pack: ${e.message}")
            }
        }
    }

    //--------------------------------------------------------------------------------------------
    // Called when the user hits the SEND button on the prompt (InputArea.kt)
    fun onSend(userText: String) {
        // First, find out if the user specifically mentioned (Hi, Hey) a particular expert in the prompt.
        val expert = findTaggedExpert(userText, state.expertsMap.values.toList())
            ?: state.currentExpert.value

        scope.launch(Dispatchers.IO) {
            if (expert == null) {
                notify("Please select or tag an expert to whom your prompt should be sent.")
                return@launch
            }

            println("Routing prompt to ${expert.nickname}.") ////

            val currentChat = state.chatManager.currentChat
            val parentNode = currentChat.currentLeafNode

            // Create and add the userNode (with prompt) and the responseNode (with empty placeholder) into
            // the currentChat.
            val userNode = Node.create(
                type = NodeType.TEXT,
                parentId = parentNode?.id,
                message = ChatMessage(MessageAuthor(type = AuthorType.USER), userText)
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
                    text = "" // Placeholder for chunked results to come from conduit
                )
            )

            // skip first of two immediate updates to the ChatsList
            state.chatManager.addNode(userNode) // unused return value
            val item = state.chatManager.addNode(responseNode)
            withContext(Dispatchers.Main) { state.chatsList.touch(item.fileName) }

            val effectiveHistory = state.chatManager.getEffectiveNodeHistory(userNode.id)
            val messages = effectiveHistory.mapNotNull { it.message }

            withContext(Dispatchers.Main) {
                responseNode.message?.textInProgress?.value = ""
            }

            state.chatManager.isAbortRequested = false
            val startTime = System.currentTimeMillis()
            expert.getResponse(state, messages)
                .onCompletion { cause ->
                    val duration = System.currentTimeMillis() - startTime
                    val status = when {
                        cause == null -> MessageStatus.COMPLETE
                        cause is CancellationException -> MessageStatus.INTERRUPTED
                        else -> MessageStatus.ERROR
                    }

                    val finalText = withContext(Dispatchers.Main) {
                        val text = responseNode.message?.textInProgress?.value ?: ""
                        responseNode.message?.textInProgress?.value = null
                        text
                    }

                    val finalNode = responseNode.copy(
                        message = responseNode.message?.copy(
                            text = finalText,
                            responseTime = duration,
                            status = status
                        )
                    )

                    val item = state.chatManager.updateNode(finalNode)
                    withContext(Dispatchers.Main) {
                        state.chatsList.touch(item.fileName)
                    }
                    if (status == MessageStatus.INTERRUPTED) {
                        notify("Response interrupted by user.")
                    }
                }
                .collect { chunk ->
                    if (state.chatManager.isAbortRequested) {
                        expert.abortResponse()
                        throw CancellationException("Aborted by user")
                    }
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
}
