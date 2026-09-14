package com.utilities.conduit

import com.utilities.conduit.debug.Trace
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.map
import kotlin.collections.toMutableList
import kotlin.math.log2

// Contains definitions, functions and helpers for both the internal ConduitUserModel
// and the ExternalUserModel, which is actually sent on LLM requests

private const val CONDUIT_USER_MODEL_FILENAME = ".conduit-user-model.json"
private const val USER_MODEL_CHAR_BUDGET = 4000
private const val USER_MODEL_HALF_LIFE_DAYS = 30.0
private const val USER_MODEL_TOTAL_WEIGHT = 0.95

/**
 * LLM-facing representation of the user model.
 * DUI is short for Deterministic Uncertainty Injection - algorithmically
 * calculated confidence measures for user features that are used to select
 * features to build the run-time user model string for LLM context.
 *
 * The run-time user model is a projection of this private ConduitUserModel
 * stored in APPDIR/.conduit-user-model.json. The persistent representation is NOT
 * passed directly to the LLM or written directly from an LLM response.
 */
@Serializable
data class UserModel(val text: String, val lastSummaryModifiedTime: Long)
{
    companion object {
        private fun conduitUserModelFile(): Path =
            Path.of(AppUtils.getAppDir()).resolve(CONDUIT_USER_MODEL_FILENAME)

        // Merge a structured LLM response into the persistent user model.
        //
        // The response is treated as candidate information only. The
        // application's internal representation and metadata remain
        // authoritative.
        //
        // Returns the updated Conduit User Model (Maint sets this into AppState)
        internal suspend fun updateConduitUserModel(
            response: String,
            lastSummaryModifiedTime: Long): ConduitUserModel {
            val conduitUserModel = loadConduitUserModelFromFile()

            // The LLM is explicitly instructed to respond with a JSON
            val externalUserModel = try {
                AppJson.decodeFromString<UserModelGenerationResponse>(response.trim())
            } catch (e: Exception) {
                Trace.log("USER MODEL: unable to parse LLM response: ${e.message}")
                return conduitUserModel
            }

            val externalFeatures = externalUserModel.features
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            val mergedFeatures = mergeFeatures(conduitUserModel.features, externalFeatures, lastSummaryModifiedTime)
            val newConduitUserModel = ConduitUserModel(mergedFeatures, lastSummaryModifiedTime)

            withContext(Dispatchers.IO) {
                val file = conduitUserModelFile()
                Files.createDirectories(file.parent)
                Files.writeString(file, AppJson.encodeToString(newConduitUserModel))
            }

            return newConduitUserModel
        }

        internal suspend fun loadConduitUserModelFromFile(): ConduitUserModel {
            val file = conduitUserModelFile()

            return withContext(Dispatchers.IO) {
                if (!Files.exists(file)) {
                    ConduitUserModel()
                } else {
                    try {
                        AppJson.decodeFromString<ConduitUserModel>(Files.readString(file))
                    } catch (e: Exception) {
                        Trace.log("USER MODEL: unable to read ${file.fileName}: ${e.message}")
                        ConduitUserModel()
                    }
                }
            }
        }

        // Merge new features into the conduit user model.
        //
        // A matching feature is currently identified by case-insensitive trimmed text. Existing
        // metadata is retained and observation information is updated.
        private fun mergeFeatures(
            currentFeatures: List<ConduitUserModelFeature>,
            newFeatureStrings: List<String>,
            observationTime: Long
        ): List<ConduitUserModelFeature> {
            val mergedFeatures = currentFeatures.toMutableList()

            for (featureString in newFeatureStrings) {
                val index = mergedFeatures.indexOfFirst {
                    it.text.trim().equals(featureString, ignoreCase = true)
                }

                if (index >= 0) {
                    val old = mergedFeatures[index]
                    mergedFeatures[index] = mergedFeatures[index].copy(
                        lastSeen = observationTime,
                        observationCount = old.observationCount + 1
                    )
                } else {
                    mergedFeatures += ConduitUserModelFeature(
                        text = featureString,
                        firstSeen = observationTime,
                        lastSeen = observationTime,
                        observationCount = 1
                    )
                }
            }

            return mergedFeatures
        }

        // Using DUI as short for Deterministic Uncertainty Injection (into the prompt)
        // DUI projection from the conduit internal representation into that exposed to the rest
        // of Conduit and ultimately supplied to the LLM.
        private const val USER_MODEL_MIN_DUI = 0.05
        internal fun convertToExternalUserModel(
            conduitUserModel: ConduitUserModel?,
            now: Long = System.currentTimeMillis()
        ): UserModel {
            if (conduitUserModel == null || conduitUserModel.features.isEmpty()) {
                return UserModel(
                    text = "NO PREVIOUS USER MODEL EXISTS",
                    lastSummaryModifiedTime = conduitUserModel?.lastSummaryModifiedTime ?: 0L
                )
            }

            val rankedFeatures = conduitUserModel.features
                .map { feature -> feature to duiScore(feature, now) }
                .filter { (_, score) -> score >= USER_MODEL_MIN_DUI }
                .sortedWith(
                    compareByDescending<Pair<ConduitUserModelFeature, Double>> { it.second }
                        .thenByDescending { it.first.observationCount }
                        .thenBy { it.first.text }
                )

            val selectedFeatures = mutableListOf<String>()
            var characterCount = 0

            // TODO review
            for ((feature, _) in rankedFeatures) {
                val separatorLength = if (selectedFeatures.isEmpty()) 0 else 1
                val additionalLength = separatorLength + feature.text.length

                if (characterCount + additionalLength > USER_MODEL_CHAR_BUDGET) {
                    continue
                }

                selectedFeatures += feature.text
                characterCount += additionalLength
            }

            val text = selectedFeatures.joinToString("\n").ifBlank { "NO PREVIOUS USER MODEL EXISTS" }
            return UserModel(text, conduitUserModel.lastSummaryModifiedTime)
        }

        private fun duiScore(feature: ConduitUserModelFeature, now: Long): Double {
            val ageMillis = (now - feature.lastSeen).coerceAtLeast(0L)
            val ageDays = ageMillis.toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
            val recencyWeight = Math.pow(0.5, ageDays / USER_MODEL_HALF_LIFE_DAYS)
            val persistenceWeight = log2(feature.observationCount.toDouble() + 1.0)

            return recencyWeight * persistenceWeight
        }

        internal data class RankedUserModelFeature(
            val feature: ConduitUserModelFeature,
            val duiScore: Double,
            val normalizedWeight: Double
        )
        internal fun rankAndNormalizeFeatures(
            features: List<ConduitUserModelFeature>,
            now: Long = System.currentTimeMillis()
        ): List<RankedUserModelFeature> {
            if (features.isEmpty()) return emptyList()

            val scoredFeatures = features
                .map { feature -> feature to duiScore(feature, now) }
                .sortedWith(
                    compareByDescending<Pair<ConduitUserModelFeature, Double>> { it.second }
                        .thenByDescending { it.first.observationCount }
                        .thenBy { it.first.text }
                )

            val totalScore = scoredFeatures.sumOf { it.second }

            if (totalScore <= 0.0) {
                return scoredFeatures.map { (feature, score) ->
                    RankedUserModelFeature(
                        feature = feature,
                        duiScore = score,
                        normalizedWeight = 0.0
                    )
                }
            }

            return scoredFeatures.map { (feature, score) ->
                RankedUserModelFeature(
                    feature = feature,
                    duiScore = score,
                    normalizedWeight = USER_MODEL_TOTAL_WEIGHT * score / totalScore
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------
/**
 * Persistent representation.
 *
 * This schema belongs entirely to Conduit and may evolve independently
 * of the external LLM-facing UserModel.
 */
@Serializable
internal data class ConduitUserModel(
    val features: List<ConduitUserModelFeature> = emptyList(),
    val lastSummaryModifiedTime: Long = 0L
)

/**
 * A single canonical user-model feature.
 *
 * The metadata is deliberately application-owned rather than LLM-owned.
 */
@Serializable
internal data class ConduitUserModelFeature(
    val text: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val observationCount: Int = 1 // Number of chat summaries this feature was seen in
)

/**
 * The only structure the LLM is currently asked to return on a user model request
 * No application metadata belongs here.
 */
@Serializable
private data class UserModelGenerationResponse(
    val features: List<String> = emptyList()
)
