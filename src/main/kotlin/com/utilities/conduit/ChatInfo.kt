package com.utilities.conduit

// Data class to represent the metadata for a historical chat session. Used to produce
// the PastChats browser panel

data class ChatInfo(
    val chatFileName: String, // Filename minus json ext
    val creationTime: Long,
    val modificationTime: Long
)
