package com.utilities.conduit

import com.utilities.conduit.AppUtils.getAppPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppActions(
    private val scope: CoroutineScope,
    private val state: AppState
) {
    //--------------------------------------------------------------------------------------------
    fun emitSystemMessage(messageText: String) {
        val infoNode = Node.create(
            type = NodeType.INFO,
            parentId = state.chatManager.currentChat.currentLeafNodeId,
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

    // Simultaneously initialize ALL experts sharing the same LLM model file path
    fun retryExpertInitialization(expert: Expert) {
        // Never purge the system expert
        if (expert.modelPath == null || expert.modelPath == state.systemExpert?.modelPath)
            return

        Expert.setStatusOfGroup(state, expert.modelPath, ExpertStatus.LOADING)

        state.expertsMap.values
            .filter { it.modelPath == expert.modelPath }
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
                val newPack = Pack.createAndLoad(newPackName, state, scope)
                state.currentPack.value = newPack
                state.currentExpert.value = null
                emitSystemMessage("The current pack is now $newPackName (You need to select a current Expert)")
            } catch (e: Exception) {
                println("Error switching pack: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------------------------------------

    // Loads all chat metadata (no messages/nodes) from disk - runs every 10-15s
    // Only needed for refresh - On individual chat updates (add and update nodes)
    // we only replace a single entry in pastChatsInfo
    fun refreshPastChatsInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatDir = java.nio.file.Paths.get(getAppPath(), "chats")
                if (!java.nio.file.Files.exists(chatDir)) return@launch

                val sortedList = java.nio.file.Files.list(chatDir)
                    .filter { it.toString().endsWith(".json") }
                    .map { path ->
                        val attrs = java.nio.file.Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes::class.java)
                        ChatInfo(
                            id = path.fileName.toString().removeSuffix(".json"),
                            creationTime = attrs.creationTime().toMillis(),
                            modificationTime = attrs.lastModifiedTime().toMillis()
                        )
                    }
                    .toList()
                    .sortedByDescending { it.modificationTime }

                withContext(Dispatchers.Main) {
                    state.pastChatsInfo.clear()
                    state.pastChatsInfo.putAll(sortedList.associateBy { it.id })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Update a single ChatInfo history entry
    fun updatePastChatsInfo(newChatInfo: ChatInfo) {
        state.pastChatsInfo[newChatInfo.id] = newChatInfo
    }

    //--------------------------------------------------------------------------------------------
    // Function to call when the user hits the SEND button on the prompt (in UI)
    fun onSend(userText: String) {
        val currentChat = state.chatManager.currentChat
        val parentId = currentChat.currentLeafNodeId

        val userNode = Node.create(
            type = NodeType.TEXT,
            parentId = parentId,
            message = ChatMessage(MessageAuthor(type = AuthorType.USER), userText)
        )
        state.chatManager.addNode(userNode)

        // Find out if the user specifically tagged a particular expert in prompt.
        val taggedExpert = findTaggedExpert(userText, state.expertsMap.values.toList())
        if (taggedExpert != null) {
            switchExpert(taggedExpert)
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
                    type = AuthorType.ASSISTANT,
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

            val prompt = messages.joinToString("\n") { msg ->
                "${msg.author.type}: ${msg.text}"
            }

            responseNode.message?.textInProgress?.value = ""
            state.chatManager.isStreaming = true // Blocks new SEND until finished

            currentExpert.getResponse(state, prompt)
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

    private fun findTaggedExpert(userText: String, experts: List<Expert>): Expert? {
        // Regex looks for: Start, optional salutation + space, then a potential name, then spaces or ,
        val regex = Regex("^\\s*(?:hi|hello|hey)\\s+(\\w+)(?:\\s*,|\\s+)?", RegexOption.IGNORE_CASE)
        val match = regex.find(userText)
        val name = match?.groupValues?.get(1) ?: return null

        return experts.find { it.nickname.equals(name, ignoreCase = true) }
    }
}
