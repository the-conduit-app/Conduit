package com.utilities.conduit

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    val text: String,
    val lastSummaryModifiedTime: Long
)
