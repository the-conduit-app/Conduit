package com.utilities.conduit

import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AppActions(
    private val scope: CoroutineScope,
    private val state: AppState
) {
    //--------------------------------------------------------------------------------------------

    fun emitSystemMessage(messageText: String) {
        val infoNode = Node.create(
            type = NodeType.INFO,
            parentId = state.chatManager.currentChat.value.currentLeafNodeId,
            message = ChatMessage(
                author = MessageAuthor(type = AuthorType.CONDUIT),
                text = messageText
            )
        )
        state.chatManager.addNode(infoNode)
    }

    fun setCurrentExpert(expert: Expert?) {
        state.currentExpert.value = expert
        val message = if (expert == null) {
            "No expert is selected currently."
        } else {
            "Current expert is now ${expert.nickname}."
        }
        emitSystemMessage(message)
    }

    fun switchExpert(newExpert: Expert) {
        if (state.currentExpert.value != newExpert)
            setCurrentExpert(newExpert)
    }

    // This simultaneously initializes ALL experts sharing the same LLM model file path
    fun retryExpertInitialization(expert: Expert) {
        val path = expert.modelPath ?: return // Return early if invalid

        Expert.clearModelFromCache(path)
        Expert.setStatusOfGroup(state, path, ExpertStatus.LOADING)

        state.expertsMap.values
            .filter { it.modelPath == path }
            .forEach { targetExpert ->
                scope.launch(Dispatchers.IO) { targetExpert.initialize(state) }
            }
    }

    // This is given just the name of the pack (from the JSON file name).
    fun switchPack(newPackName: String) {
        if (state.currentPack.value?.name == newPackName) return

        scope.launch(Dispatchers.IO) {
            try {
                state.expertsMap.clear()
                val newPack = Pack.load(newPackName, state, scope)
                state.currentPack.value = newPack
                state.currentExpert.value = null
                emitSystemMessage("The current pack is now $newPackName (You need to select a current Expert)")
            } catch (e: Exception) {
                println("Error switching pack: ${e.message}")
            }
        }
    }

    //--------------------------------------------------------------------------------------------
    // Function to call when the user hits the SEND button on the prompt (in UI)
    fun onSend(userText: String) {
        val currentChat by state.chatManager.currentChat
        val parentId = currentChat.currentLeafNodeId

        val userNode = Node.create(
            type = NodeType.TEXT,
            parentId = parentId,
            message = ChatMessage(MessageAuthor(type = AuthorType.USER), userText)
        )
        state.chatManager.addNode(userNode)

        // Find out if the user specifically tagged a particular expert with "Hi ", "Hello" etc.
        val taggedExpert = findTaggedExpert(userText, state.expertsMap.values.toList())
        when (taggedExpert) {
            is TaggedExpert.Found -> switchExpert(taggedExpert.expert)
            is TaggedExpert.Unknown -> {
                emitSystemMessage("Sorry! '${taggedExpert.name}' not in chosen pack")
                return
            }
            is TaggedExpert.None -> { /* current expert remains current */ }
        }

        val currentExpert = state.currentExpert.value // Capture
        if (currentExpert == null) {
            emitSystemMessage("Please select or tag an expert to whom your prompt should be sent.")
            return
        }

        val responseNode = Node.create(
            type = NodeType.TEXT,
            parentId = userNode.id,
            message = ChatMessage(
                author = MessageAuthor(
                    type = AuthorType.EXPERT,
                    expertId = currentExpert.id,
                    packId = state.currentPack.value?.id
                ),
                text = "" // Placeholder for chunked results
            )
        )
        state.chatManager.addNode(responseNode)

        scope.launch(Dispatchers.IO) {
            val context = state.chatManager.getEffectiveNodeHistory(userNode.id)
            val messages = context.mapNotNull { it.message }
            val startTime = System.currentTimeMillis()

            responseNode.message?.textInProgress?.value = ""
            state.chatManager.isStreaming = true // Blocks new SEND until finished

            currentExpert.getResponse(messages)
                .onCompletion {
                    val duration = System.currentTimeMillis() - startTime

                    val finalText = responseNode.message?.textInProgress?.value ?: ""
                    val finalNode = responseNode.copy(
                        message = responseNode.message?.copy(text = finalText, responseTime = duration)
                    )
                    responseNode.message?.textInProgress?.value = null
                    state.chatManager.updateNode(finalNode)
                    state.chatManager.isStreaming = false
                }
                .collect { chunk ->
                    for (char in chunk) {
                        responseNode.message?.textInProgress?.value += char
                        //delay(50.milliseconds) -- commented out cuz of lost chars in the flow
                    }
                }
        }
    }

    //--------------------------------------------------------------------------------------------
    // Utils for onSend

    private fun findTaggedExpert(userText: String, experts: List<Expert>): TaggedExpert {
        // Regex looks for: Start, optional salutation + space, then a potential name, then spaces or ,
        val regex = Regex("^\\s*(?:hi|hello|hey)\\s+(\\w+)(?:\\s*,|\\s+)?", RegexOption.IGNORE_CASE)
        val match = regex.find(userText)

        val name = match?.groupValues?.get(1) ?: return TaggedExpert.None

        val expert = experts.find { it.nickname.equals(name, ignoreCase = true) }
        return if (expert != null) TaggedExpert.Found(expert) else TaggedExpert.None
    }

    private sealed class TaggedExpert {
        data class Found(val expert: Expert) : TaggedExpert()
        data class Unknown(val name: String) : TaggedExpert()
        object None : TaggedExpert()
    }
}
