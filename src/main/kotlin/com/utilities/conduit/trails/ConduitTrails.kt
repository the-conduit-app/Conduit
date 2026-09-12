package com.utilities.conduit.trails

import com.utilities.conduit.debug.Trace
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Trails maintains a cursor through a tree of predefined conversational
// sequences.
//
// Each TrailNode contains a string and a list of child TrailNodes.
//
// The trails file consists of sections separated by a line containing
// "End Of Section". Each section is simply a sequence of strings:
//
//     prompt 1
//     response 1
//     prompt 2
//     response 2
//     ...
//
// Sections are overlaid into a single tree. Shared sequences therefore share
// the same TrailNodes, while diverging sequences create branches.
//
// Example:
//
//     root
//       └── "To be or not to be"
//             └── "To be or not to be; that is the question"
//                   └── ...
//
// ConduitExpert.getResponse() keeps a Trails instance and, after trying its
// stock responses, makes one final attempt to advance the trail using the
// user's prompt.
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
//   - return the End Of Trail reward.
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
    val TRAILS_KEY = "       2_       ".toByteArray(Charsets.UTF_8)

    fun initialize() {
        root = ConduitTrailNode("Trails")
        cursor = root

        val stream = ConduitTrails::class.java.getResourceAsStream(TRAILS_RESOURCE)
        if (stream == null) {
            Trace.log("No Conduit trails found")
            return
        }

        val encrypted = stream.use { it.readBytes() }
        if (encrypted.size < 12 + 16) {
            Trace.log("Conduit trails could not be loaded")
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

        val plaintext = cipher
            .doFinal(ciphertextAndTag)
            .toString(Charsets.UTF_8)

        val trail = mutableListOf<String>()

        plaintext.lineSequence().forEach { line ->
            val text = line.trim()

            if (text == "End of Trail") {
                if (trail.isNotEmpty()) {
                    addTrail(trail)
                    trail.clear()
                }
            } else if (text.isNotEmpty()) {
                trail.add(text)
            }
        }

        if (trail.isNotEmpty()) {
            addTrail(trail)
        }

        root.children.forEach { it -> println("Loaded Trail: ${it.text}") }
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

    fun advance(prompt: String): String? {
        // Trail has not been initialized.
        if (!::root.isInitialized) {
            return null
        }

        // Advance #1: consume the user's prompt.
        val promptNode = cursor.children.firstOrNull {
            it.text.equals(prompt.trim(), ignoreCase = true)
        } ?: run {
            // No matching prompt at this point in the trail.
            // Abandon the current trail and start again from the root.
            cursor = root
            return null
        }

        cursor = promptNode

        // The matching prompt is a leaf, and so is at end of trail
        // The trail has ended.
        if (cursor.children.isEmpty()) {
            cursor = root
            return "Yippee!"
        }

        // Advance #2: select a response node.
        cursor = cursor.children.random()

        // If the response node is a leaf, this response completes the trail.
        if (cursor.children.isEmpty()) {
            val response = "${cursor.text}\n\nYippee!"
            cursor = root
            return response
        }

        return cursor.text
    }
}
