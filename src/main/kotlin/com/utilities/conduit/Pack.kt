package com.utilities.conduit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Pack(
    val name: String = "",
    val description: String = "",
    val experts: List<Expert>
) {
    @Transient
    val id: String = java.util.UUID.randomUUID().toString()

    companion object {
        // Ensure call only from IO thread
        suspend fun createAndLoad(packFile: String, state: AppState, scope: CoroutineScope): Pack {
            val packPath = java.nio.file.Path.of(AppUtils.getAppPath(), "packs", packFile)

            if (!java.nio.file.Files.exists(packPath)) {
                throw IllegalStateException("Pack not found at: $packPath")
            }
            val jsonContent = java.nio.file.Files.readString(packPath)
            val pack = AppJson.decodeFromString<Pack>(jsonContent)

            // Load ASYNC blocking (one expert AFTER another)
            pack.experts.forEach { expert ->
                if (expert.type == ExpertType.INTERNAL) {
                    expert.status = ExpertStatus.READY
                } else {
                    expert.initialize(state)
                }
            }

            withContext(Dispatchers.Main) {
                state.expertsMap.clear()
                state.expertsMap.putAll(pack.experts.associateBy { it.id })
                state.currentPack.value = pack
            }

            return pack
        }
    }
}
