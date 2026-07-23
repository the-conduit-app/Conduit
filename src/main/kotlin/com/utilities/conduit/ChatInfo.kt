package com.utilities.conduit

// Data class to represent the metadata for a historical chat session. Used to produce
// the PastChats browser panel

data class ChatInfo(
    val id: String, // Filename minus json ext
    val creationTime: Long,
    val modificationTime: Long
) {
    val name: String get() = id                    // Displayed in chats menu
    val fileName: String get() = "$id.json"
}
