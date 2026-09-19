package com.utilities.conduit.portals

import com.utilities.conduit.AppJson
import com.utilities.conduit.ConduitUserModel
import com.utilities.conduit.Expert
import com.utilities.conduit.UserModel
import com.utilities.conduit.trails.ConduitTrails
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

object ConduitPortal {
    private var cancelRequested = false
    private val conduitTrails = ConduitTrails()

    fun initializeTrails() {
        conduitTrails.initialize()
    }

    internal fun getResponse(expert: Expert, prompt: String): Flow<String> {
        cancelRequested = false
        val conduitUserModel = expert.conduitUserModelProvider()

        val response = getConduitResponse(prompt.trim(), conduitUserModel)

        return flow {
            for (char in response) {
                if (cancelRequested) {
                    cancelRequested = false
                    throw CancellationException("Conduit response canceled")
                }

                emit(char.toString())
                delay(10.milliseconds)
            }
        }
    }

    fun abortResponse() {
        cancelRequested = true
    }

    internal fun getConduitResponse(prompt: String, conduitUserModel: ConduitUserModel?): String {
        return when {
            prompt.equals("What do you know about me?", ignoreCase = true) -> {
                val res = getFriendlyExternalUserModel(conduitUserModel)
                println ("Response = $res")
                res
            }

            prompt.equals("What do you really know about me?", ignoreCase = true) -> {
                getFriendlyConduitUserModel(conduitUserModel)
            }

            prompt.equals("What do you really really know about me?", ignoreCase = true) -> {
                AppJson.encodeToString(conduitUserModel)
            }

            prompt.equals("Thanks", ignoreCase = true) || prompt.equals("Thank you", ignoreCase = true) -> {
                "You're welcome."
            }

            prompt.equals("Sorry", ignoreCase = true) || prompt.equals("I'm sorry", ignoreCase = true) -> {
                "No worries."
            }

            prompt.equals("Help", ignoreCase = true) -> {
                "You are in the Conduit. You can hold a local (no Internet) round-table discussion with the various experts " +
                        "in Conduit Packs.\n\nThey can help you cooperatively, understand each other well, " +
                        "and are aware of each other's responses.\n\n" +
                        "The rest of Conduit is for you to explore and discover."
            }

            prompt.equals("More Help", ignoreCase = true) || prompt.equals("Moar Help", ignoreCase = true) -> {
                "Gemma4 (gemma-2-9b-it-Q4_K_M.gguf, SHA = '13b2a7b4115bbd0900162edcebe476da1ba1fc24e718e8b40d32f6e300f56dfe)' " +
                        "is the key to open the portal. You obviously already know that!\n\n" +
                        "Once inside, you can:\n"+
                        "  - chat with multiple experts in packs.\n" +
                        "  - carry on a real branching conversation, and view the tree.\n" +
                        "  - teleport from place to place in your chat\n\n" +
                        "The support folder for Conduit is '~/Library/Application Support/Conduit'\n" +
                        "  - All your chats can be found in the chats/ folder there\n" +
                        "  - Place new pack definitions in the packs/ folder\n" +
                        "  - Use approved LLM files (.gguf) placed in the llm/ folder (or desktop)\n\n" +
                        "Enjoy!"
            }

            prompt.equals("Still more Help", ignoreCase = true) -> {
                "Go forth and explore! Discover new stuff."
            }

            else -> {
                val wasAtRoot = conduitTrails.isAtRoot()
                val response = conduitTrails.advance(prompt)
                response ?: if (!wasAtRoot) {
                    "Ouch!"
                } else {
                    "Hello, I am Condy, the Conduit. I'm not really an expert and " +
                            "I can only answer the following question: 'What do you know about me?'"
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers

    // Returns the internal Conduit User Model as a chat-message-worthy string.
    // Features are ranked by DUI score and displayed with their relative
    // deterministic confidence.
    //
    // Example:
    //   51% confidence: Mathematics
    //   48% confidence: Astronomy
    //   ...
    private fun getFriendlyConduitUserModel(conduitUserModel: ConduitUserModel?): String {
        if (conduitUserModel == null || conduitUserModel.features.isEmpty()) {
            return "NO USER MODEL EXISTS"
        }

        val rankedFeatures = UserModel.rankAndNormalizeFeatures(conduitUserModel.features)
        return rankedFeatures.joinToString("\n") { ranked ->
            "${(ranked.normalizedWeight * 100.0).toInt()}% confidence: ${ranked.feature.text}"
        }
    }

    // Returns a kinda grammatical sentence made from the externalUserModel, which is
    // extracted from the passed ConduitUserModel
    private fun getFriendlyExternalUserModel(conduitUserModel: ConduitUserModel?): String {
        val userModel = UserModel.convertToExternalUserModel(conduitUserModel)
        val features = userModel.text.lines().filter { it.isNotBlank() }

        fun extractFeatures(prefixes: List<String>): List<String> =
            features
                .filter { feature -> prefixes.any { feature.startsWith(it) } }
                .map { feature ->
                    val prefix = prefixes.first { feature.startsWith(it) }
                    feature.removePrefix(prefix).removeSuffix(".")
                }

        val interests = extractFeatures(listOf("Interested in "))
        val enjoys = extractFeatures(listOf("Enjoys "))
        val preferences = extractFeatures(listOf("Prefers "))
        val names = extractFeatures(listOf("Has name ", "Is named "))

        val sentences = mutableListOf<String>()

        if (interests.isNotEmpty()) {
            sentences += "You are interested in ${joinNaturally(interests)}."
        }
        if (enjoys.isNotEmpty()) {
            sentences += "You enjoy ${joinNaturally(enjoys)}."
        }
        if (preferences.isNotEmpty()) {
            sentences += "You prefer ${joinNaturally(preferences)}."
        }
        if (names.isNotEmpty()) {
            sentences += "Your name is ${names.first()}."
        }

        return sentences.joinToString(" ")
    }

    private fun joinNaturally(items: List<String>): String =
        when (items.size) {
            0 -> ""
            1 -> items[0]
            2 -> "${items[0]} and ${items[1]}"
            else -> items.dropLast(1).joinToString(", ") + ", and ${items.last()}"
        }
}
