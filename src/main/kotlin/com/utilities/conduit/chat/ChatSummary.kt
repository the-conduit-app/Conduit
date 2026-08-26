package com.utilities.conduit.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatSummary(
    val chatId: String,
    val chatTitle: String,
    val sourceModifiedTime: Long,
    val summary: String
)
