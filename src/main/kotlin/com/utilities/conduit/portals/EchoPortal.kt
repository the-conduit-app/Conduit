/* All kinds of Internal Echo Experts. They all have a modelPath like
 * ":internal:SimpleEcho"
 */
package com.utilities.conduit.portals

import com.utilities.conduit.Expert
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

object EchoPortal {
    private var cancelRequested = false

    suspend fun getResponse(expert: Expert, text: String): Flow<String> {
        val prompt = text.trim()

        cancelRequested = false
        val response = when (expert.model) {
            ":simpleEcho" -> getSimpleResponse(prompt)
            ":saladEcho" -> getSaladResponse(prompt)
            ":ohceEcho" -> getOhceEchoResponse(prompt)
            ":rottenEcho" -> getRottenResponse(prompt)
            ":sillyEcho" -> getSillyResponse(prompt)
            else -> getSimpleResponse(prompt)
        }

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

    fun getSimpleResponse(text: String?): String {
        return (text?: "Silence")
    }

    fun getSaladResponse(text: String?): String {
        return (text ?: "").split(" ").shuffled().joinToString(" ")
    }

    fun getOhceEchoResponse(text: String?): String {
        return (text ?: "").split(" ").reversed().joinToString(" ")
    }

    fun getRottenResponse(text: String?): String {
        return (text ?: "").map {
            when (it) {
                in 'a'..'m', in 'A'..'M' -> it + 13
                in 'n'..'z', in 'N'..'Z' -> it - 13
                else -> it
            }
        }.joinToString("")
    }

    fun getSillyResponse(text: String?): String {
        val flipTable = mapOf(
            'a' to 'ɐ', 'b' to 'q', 'c' to 'ɔ', 'd' to 'p', 'e' to 'ǝ', 'f' to 'ɟ', 'g' to 'ƃ',
            'h' to 'ɥ', 'i' to 'ᴉ', 'j' to 'ɾ', 'k' to 'ʞ', 'l' to 'l', 'm' to 'ɯ', 'n' to 'u',
            'o' to 'o', 'p' to 'd', 'q' to 'b', 'r' to 'ɹ', 's' to 's', 't' to 'ʇ', 'u' to 'n',
            'v' to 'ʌ', 'w' to 'ʍ', 'x' to 'x', 'y' to 'ʎ', 'z' to 'z',
            'A' to '∀', 'B' to 'B', 'C' to 'Ɔ', 'D' to 'Ɑ', 'E' to 'Ǝ', 'F' to 'Ⅎ', 'G' to 'פ',
            'H' to 'H', 'I' to 'I', 'J' to 'ſ', 'K' to 'ʞ', 'L' to '˥', 'M' to 'W', 'N' to 'N',
            'O' to 'O', 'P' to 'Ԁ', 'Q' to 'Ό', 'R' to 'ᴚ', 'S' to 'S', 'T' to '┴', 'U' to '∩',
            'V' to 'Λ', 'W' to 'M', 'X' to 'X', 'Y' to '⅄', 'Z' to 'Z',
            '?' to '¿', '!' to '¡', '.' to '˙', ',' to '\''
        )
        return (text ?: "").map { flipTable[it] ?: it }.reversed().joinToString("")
    }
}
