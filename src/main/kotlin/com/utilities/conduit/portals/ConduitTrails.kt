package com.utilities.conduit.portals
/*
import com.utilities.conduit.chat.*
import jdk.internal.net.http.common.Pair.pair

// A Conduit Trail is a special pre-existing branching chat whose json start with a .
// It is invisible to the standard visible observed Chats list.
// As documented, the :conduit system expert only answers the following questions:
//     1. "What do I know about you" -> { getUserModelAsResponseString() }
//     2. "?" -> same as above.
// However, a ConduitTrail is a hidden" easter-egg chat that can be navigated (read-only)
// in by prompting with EXACTLY the string stored in the currentCursor's childrens'
// messages.text field. If it matches, then the cursor is advanced along the matching
// branch. If the prompt is ANYTHING other than an exact match, then the cursor resets
// to the beginning of the chat (root).
//

data class ConduitTrail(
    val chat: Chat,
)

// Updates the given Chat in memory only such that it has the list of
// q/a pairs as a path from its root. E.g. If the Chat is currently
// ABCDEF and updateTrailChat(chat, [ <A,B>, <C,D>, <G, H>, <I,J> ])
// then the in-memory special chat is updated to include a new branch
// off node D. By calling updateTrailChat() on many such Q/A pair lists
// we initialize the ConduitTrail object to be used by the conduitExpert.
// The way it works is this:
// When the user sends the conduit expert (condy) a prompt, get response
// (which maintains a static ConduitTrail object), will compare the prompt
// with trail's chat's current node's (cursor's) childrens' messages.
// If it matches *EXACTLY* with one of it's children's messages, then the
// chat's cursor is advanced to that node. (Reaching the end of the any
// trail elicits a "Hooray!" response and resetting the cursor back to
// the root. If the prompt matches NONE of the cursor's children, then
// it simply returns "Ok" or sth and returns to the root again.

val examplePath : List<String> = {
    "To be or not to be, that is the question",
    "Whether 'tis nobler in the mind to suffer",
    "The slings and arrows of outrageous fortune",
    "Or to take arms against a sea of troubles",
    // Please fill in the other pairs from the passage like the above
}
fun updateTrailChat(chat: Chat, qaPath: List<Pair<String, String>>) {
    // This does the equivalent of addNode to a regular chat (except not save it, or notify chatslist etc.)
    // Simply adds the nodes to the given chat in the correct places and returns
    // So at the end of this call, this chat contains the given qaPath as one of the valid paths
    // from root.
}

// The caller ConduitPortal.getResponse("prompt") maintains a static ConduitTrail object
// with this special chat in it. For each incoming prompt, it attempts to nagivate the current
// chat from the current cursor using the current prompt (READ-ONLY). If the prompt successfully
// matches against any child, (1) the cursor is advanced to that child (2) if the child has only one
// descendant, then the descendant's message is output and the cursor advances again (3) If the cursor
// has navigated to the leaf then returns "Hooray!" and resets the cursor to the root node.



 // Find an existing question child with exactly this text.
 val existingQuestion = parentId?.let { id ->
     chat.nodes[id]
         ?.children
         ?.mapNotNull(chat.nodes::get)
         ?.firstOrNull { node ->
             node.message.author == "user" &&
             node.message.text == question
         }
 }

 val questionNode = existingQuestion ?: run {
     val node = Node(
         id = UUID.randomUUID().toString(),
         message = /* user message containing question */,
         parentId = parentId
     )

     chat.nodes[node.id] = node

     parentId?.let { id ->
         chat.nodes[id]?.children?.add(node.id)
     }

     node
 }

 // Find or create the answer belonging to this question.
 val existingAnswer = questionNode.children
     .mapNotNull(chat.nodes::get)
     .firstOrNull { node ->
         node.message.author == "assistant" &&
         node.message.text == answer
     }

 val answerNode = existingAnswer ?: run {
     val node = Node(
         id = UUID.randomUUID().toString(),
         message = /* assistant message containing answer */,
         parentId = questionNode.id
     )

     chat.nodes[node.id] = node
     questionNode.children.add(node.id)

     node
 }

 parentId = answerNode.id
 */
