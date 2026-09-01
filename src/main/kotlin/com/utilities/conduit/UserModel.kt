package com.utilities.conduit

import com.utilities.conduit.debug.Trace
import com.utilities.conduit.ui.AppJson
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path

/**
 * LLM-facing representation of the user model.
 * DUI short for Deterministic Uncertainty Injection
 *
 * This is a projection of the private ConduitUserModel stored in
 * .user-model.json. The persistent representation must never be
 * passed directly to the LLM or written directly from an LLM response.
 */
@Serializable
data class UserModel(
    val text: String,
    val lastSummaryModifiedTime: Long
) {
    companion object {
        private const val FILE_NAME = ".user-model.json"

        /**
         * Return the current user model in its LLM-facing form.
         *
         * This is the DUI boundary. For now all stored features are exposed.
         * The feature-selection/weighting algorithm can become more
         * sophisticated later without changing callers.
         */
        suspend fun getUserModel(): UserModel {
            val stored = readStoredModel()
            return projectToUserModel(stored)
        }

        /**
         * Merge a structured LLM response into the persistent user model.
         *
         * The response is treated as candidate information only. The
         * application's internal representation and metadata remain
         * authoritative.
         */
        suspend fun saveUserModel(
            response: String,
            lastSummaryModifiedTime: Long
        ): UserModel {
            val existing = readStoredModel()

            val generation = try {
                AppJson.decodeFromString<UserModelGenerationResponse>(
                    response.trim()
                )
            } catch (e: Exception) {
                Trace.log(
                    "USER MODEL: unable to parse generation response: ${e.message}"
                )
                return projectToUserModel(existing)
            }

            val now = System.currentTimeMillis()

            val candidates = generation.features
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            val mergedFeatures = mergeFeatures(
                existing.features,
                candidates,
                now
            )

            val updated = ConduitUserModel(
                features = mergedFeatures,
                lastSummaryModifiedTime = lastSummaryModifiedTime
            )

            writeStoredModel(updated)

            return projectToUserModel(updated)
        }

        private suspend fun readStoredModel(): ConduitUserModel {
            val file = userModelFile()

            return withContext(Dispatchers.IO) {
                if (!Files.exists(file)) {
                    ConduitUserModel()
                } else {
                    try {
                        AppJson.decodeFromString<ConduitUserModel>(
                            Files.readString(file)
                        )
                    } catch (e: Exception) {
                        Trace.log(
                            "USER MODEL: unable to read ${file.fileName}: ${e.message}"
                        )
                        ConduitUserModel()
                    }
                }
            }
        }

        private suspend fun writeStoredModel(model: ConduitUserModel) {
            val file = userModelFile()

            withContext(Dispatchers.IO) {
                Files.createDirectories(file.parent)

                Files.writeString(
                    file,
                    AppJson.encodeToString(model)
                )
            }
        }

        private fun userModelFile(): Path =
            Path.of(AppUtils.getAppDir()).resolve(FILE_NAME)

        /**
         * Merge candidate features into the canonical model.
         *
         * A matching feature is currently identified by case-insensitive
         * trimmed text. Existing metadata is retained and observation
         * information is updated.
         */
        private fun mergeFeatures(
            existing: List<UserModelFeature>,
            candidates: List<String>,
            now: Long
        ): List<UserModelFeature> {
            val merged = existing.toMutableList()

            for (candidate in candidates) {
                val index = merged.indexOfFirst {
                    it.text.trim().equals(candidate, ignoreCase = true)
                }

                if (index >= 0) {
                    val old = merged[index]

                    merged[index] = old.copy(
                        lastSeen = now,
                        occurrences = old.occurrences + 1
                    )
                } else {
                    merged += UserModelFeature(
                        text = candidate,
                        firstSeen = now,
                        lastSeen = now,
                        occurrences = 1
                    )
                }
            }

            return merged
        }

        /**
         * DUI projection from the canonical internal representation into
         * the representation exposed to the rest of Conduit and ultimately
         * supplied to the LLM.
         *
         * For now every feature is exposed. This is deliberately the simple
         * first implementation of DUI.
         */
        private fun projectToUserModel(
            stored: ConduitUserModel
        ): UserModel {
            val text = stored.features
                .joinToString("\n") { it.text }
                .ifBlank { "NO PREVIOUS USER MODEL EXISTS" }

            return UserModel(
                text = text,
                lastSummaryModifiedTime = stored.lastSummaryModifiedTime
            )
        }
    }
}

// --------------------------------------------------------------------------------------

/**
 * Private persistent representation.
 *
 * This schema belongs entirely to Conduit and may evolve independently
 * of the LLM-facing UserModel.
 */
@Serializable
private data class ConduitUserModel(
    val features: List<UserModelFeature> = emptyList(),
    val lastSummaryModifiedTime: Long = 0L
)


/**
 * A single canonical user-model feature.
 *
 * The metadata is deliberately application-owned rather than LLM-owned.
 */
@Serializable
private data class UserModelFeature(
    val text: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val occurrences: Int = 1
)


/**
 * The only structure Gem is currently asked to return.
 *
 * No application metadata belongs here.
 */
@Serializable
private data class UserModelGenerationResponse(
    val features: List<String> = emptyList()
)
