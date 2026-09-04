package com.utilities.conduit.portals

import com.utilities.conduit.ConduitUserModel
import com.utilities.conduit.Expert
import com.utilities.conduit.ExternalUserModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

object ConduitPortal {
    private var cancelRequested = false

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
                delay(25.milliseconds)
            }
        }
    }

    fun abortResponse() {
        cancelRequested = true
    }

    internal fun getConduitResponse(prompt: String, conduitUserModel: ConduitUserModel?): String {
        return when {
            prompt.equals("What do you know about me?", ignoreCase = true) -> {
                getFriendlyExternalUserModel(conduitUserModel)
            }

            prompt.equals("What do you really know about me?", ignoreCase = true) -> {
                getFriendlyConduitUserModel(conduitUserModel)
            }

            else ->
                "Hello, I am Condy, the Conduit. I'm not really an expert and I can only answer\n" +
                        "the following question: 'What do you know about me?'"
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

        val rankedFeatures = ExternalUserModel.rankAndNormalizeFeatures(conduitUserModel.features)
        return rankedFeatures.joinToString("\n") { ranked ->
            "${(ranked.normalizedWeight * 100.0).toInt()}% confidence: ${ranked.feature.text}"
        }
    }

    // Returns a kinda grammatical sentence made from the externalUserModel, which is
    // extracted from the passed ConduitUserModel
    private fun getFriendlyExternalUserModel(conduitUserModel: ConduitUserModel?): String {
        val externalUserModel = ExternalUserModel.convertToExternalUserModel(conduitUserModel)
        val features = externalUserModel.text.lines().filter { it.isNotBlank() }

        val interests = features
            .filter { it.startsWith("Interest in ") }
            .map { it.removePrefix("Interest in ").removeSuffix(".") }

        val sentences = mutableListOf<String>()
        if (interests.isNotEmpty()) {
            sentences += "You are interested in ${joinNaturally(interests)}."
        }

        // TODO: omitting preferences for now since it seems incorrect

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
