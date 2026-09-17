package com.utilities.conduit

import kotlinx.serialization.Serializable

// These are the only models loadable into Conduit packs (to prevent noobie configs using buggy models)
@Serializable
data class ApprovedModel(
    val name: String,
    val description: String
)
