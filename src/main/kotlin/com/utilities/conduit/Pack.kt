package com.utilities.conduit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Pack(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val experts: List<Expert>
) {
    @kotlinx.serialization.Transient
    lateinit var name: String

    private fun initializeExperts(state: AppState, scope: CoroutineScope) {
        experts.forEach { expert ->
            if (expert.type == ExpertType.ECHO) {
                expert.status = ExpertStatus.READY
            } else {
                scope.launch(Dispatchers.IO) {
                    expert.initialize(state)
                }
            }
        }
    }

    companion object {
        suspend fun load(packName: String, state: AppState, scope: CoroutineScope): Pack {
            val packFile = File(GeneralUtils.getAppDir(), "packs/$packName.json")
            if (!packFile.exists()) {
                throw IllegalStateException("Pack not found at: ${packFile.absolutePath}")
            }

            val jsonContent = packFile.readText()
            val pack = Json.decodeFromString<Pack>(jsonContent)
            pack.name = packName

            withContext(Dispatchers.Main) {
                state.expertsMap.clear()
                state.expertsMap.putAll(pack.experts.associateBy { it.id })
                state.currentPack.value = pack
            }

            // Load ASYNC
            pack.initializeExperts(state, scope)

            return pack
        }
    }
}
