package com.utilities.conduit.chat

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ChatSummary(
    val chatId: String,
    val chatTitle: String,
    val chatModifiedTime: Long,
    val summary: String
)
