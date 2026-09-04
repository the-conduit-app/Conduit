package com.utilities.conduit.debug

/*
    // DEBUG/TESTING related --------------------------------------------------------------------
    internal suspend fun getDuiRankingForTest(now: Long = System.currentTimeMillis()): List<Pair<String, Double>> {
        val conduitUserModel = loadConduitUserModelFromFile()

        return conduitUserModel.features
            .map { feature -> feature.text to duiScore(feature, now) }
            .sortedWith(
                compareByDescending<Pair<String, Double>> { it.second }.thenBy { it.first }
            )
    }
    internal fun duiTest(features: List<ConduitUserModelFeature>, now: Long): Pair<List<Triple<Int, Double, ConduitUserModelFeature>>, UserModel> {
        val stored = ConduitUserModel(
            features = features,
            lastSummaryModifiedTime = now
        )

        val rankedFeatures = stored.features
            .map { feature -> feature to duiScore(feature, now) }
            .sortedWith(
                compareByDescending<Pair<ConduitUserModelFeature, Double>> { it.second }
                    .thenByDescending { it.first.observationCount }
                    .thenBy { it.first.text }
            )
            .mapIndexed { index, (feature, score) -> Triple(index + 1, score, feature) }

        return rankedFeatures to convertToExternalUserModel(stored, now)
    }

 */
