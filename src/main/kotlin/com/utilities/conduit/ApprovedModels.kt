package com.utilities.conduit

import kotlinx.serialization.Serializable
import java.io.File
import java.io.Serial

// These are the only models loadable into Conduit packs (to prevent noobie configs using buggy models)
@Serializable
data class ApprovedModel(
    val name: String,
    val description: String
)
