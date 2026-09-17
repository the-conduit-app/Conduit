package com.utilities.conduit.trails

import com.utilities.conduit.debug.Trace
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Just a fun diversion - not core functionality:
//
// Trails maintains a cursor through a tree of predefined conversational
// sequences. To progress easily, one can solve Quests in Questopia to
// discover safe spots in the trail (Going anywhere else means doom)
//
// Each TrailNode contains a string and a list of child TrailNodes.
//
// The trails data consists of sections separated by a line containing
// "End Of Section". Each section is simply a sequence of strings:
//
//     str 1
//     str 2
//     ...
//
// Sections are overlaid into a single tree. Shared sequences therefore share
// the same TrailNodes, while diverging sequences create branches.
//
// Example:
//
//     root
//       └── "To be or not to be"
//       |      └── "To be or not to be; that is the question"
//       |           └── ...
//       └── "Questopia"
//       |     └── "A Tiger named Fangs"
//      ...
//
// ConduitExpert.getResponse() keeps a Trails instance and, after trying its
// stock responses ("What do you know about me?", etc.), makes one final attempt
// to get a wacky response by trying to advance on the trail using the user's prompt.
//
// advance(prompt) performs two possible advances:
//
//   1. Find a child of the current cursor whose text matches the prompt.
//      If found, advance the cursor to that node.
//
//   2. If that node has a child, select one of its children, advance the
//      cursor to that child, and return that child's text as Conduit's
//      response.
//
// If the prompt does not match any child of the current cursor:
//   - reset the cursor to root
//   - return null
//
// If either advance reaches a leaf, the trail has ended:
//   - reset the cursor to root
//   - return the End Of Trail reward (Yippee)
//
// Matching is case-insensitive.

// A TrailNode contains one string and zero or more possible next strings.
class ConduitTrailNode(val text: String) {
    val children: MutableList<ConduitTrailNode> = mutableListOf()
}

class ConduitTrails {
    private lateinit var root: ConduitTrailNode
    private lateinit var cursor: ConduitTrailNode
    val TRAILS_RESOURCE = "/assets/ConduitTrails.enc"
    val TRAILS_KEY = "உ             ".toByteArray(Charsets.UTF_8)

    fun initialize() {
        root = ConduitTrailNode("Trails")
        cursor = root

        val stream = ConduitTrails::class.java.getResourceAsStream(TRAILS_RESOURCE)
        if (stream == null) {
            Trace.log("No Conduit trails found - can't go hiking.")
            return
        }

        val encrypted = stream.use { it.readBytes() }
        if (encrypted.size < 12 + 16) {
            Trace.log("Conduit trails could not be loaded - park that hike.")
            return
        }

        val nonce = encrypted.copyOfRange(0, 12)
        val ciphertextAndTag = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(TRAILS_KEY, "AES"),
            GCMParameterSpec(128, nonce)
        )

        val plaintext = cipher.doFinal(ciphertextAndTag).toString(Charsets.UTF_8)

        val trail = mutableListOf<String>()
        for (line in plaintext.lineSequence()) {
            val text = line.trim()

            if (text.isBlank()) continue

            if (text == "End of Trail") {
                if (trail.isNotEmpty()) {
                    addTrail(trail)
                    trail.clear()
                }
            } else {
                trail.add(text)
            }
        }

        if (trail.isNotEmpty()) {
            addTrail(trail)
        }
    }

    private fun addTrail(trail: List<String>) {
        var node = root
        for (text in trail) {
            val child = node.children.firstOrNull { it.text == text } ?: ConduitTrailNode(text).also {
                node.children.add(it)
            }
            node = child
        }
    }

    fun isAtRoot(): Boolean {
        return cursor == root
    }

    fun advance(prompt: String): String? {
        // Trail has not been initialized.
        if (!::root.isInitialized) {
            return null
        }

        // Advance #1: consume the user's prompt if it matches
        val promptNode = cursor.children.firstOrNull {
            it.text.equals(prompt.trim(), ignoreCase = true)
        } ?: run {
            // No matching prompt at this point in the trail.
            // Abandon the current trail and start again from the root.
            cursor = root
            return null
        }

        cursor = promptNode

        // If the matching prompt is a leaf, this is the end of the trail
        if (cursor.children.isEmpty()) {
            cursor = root
            return "Yippee!"
        }

        // Advance #2: select a response node.
        cursor = cursor.children.random()

        // If this response node is a leaf, it also completes the trail.
        if (cursor.children.isEmpty()) {
            val response = "${cursor.text}\n\nYip, Yip, Yippee!"
            cursor = root
            return response
        }

        return cursor.text
    }

    // Debuggies -----------------------------------------------------------------------

    fun printEntries() {
        root.children.forEach { it -> println("Loaded Trail: ${it.text}") }
    }

    fun printTrail() {
        println(root.text)
        printChildren(root, "")
    }

    private fun printChildren(node: ConduitTrailNode, indent: String) {
        for (child in node.children) {
            println("$indent${child.text}")
            printChildren(child, "$indent    ")
        }
    }

}
