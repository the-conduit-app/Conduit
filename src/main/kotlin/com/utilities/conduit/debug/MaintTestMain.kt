package com.utilities.conduit.debug

/*
 * Comment in for testing - may require slight refactoring to adjust for
 * changes in the rest of the code.

import com.utilities.conduit.AppState
import com.utilities.conduit.Maintenance
import com.utilities.conduit.UserModel
import com.utilities.conduit.ConduitUserModelFeature
import com.utilities.conduit.chat.ChatsList
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText

fun main(args: Array<String>) = runBlocking {
    val mode = args.firstOrNull()?.lowercase() ?: "all"

    if (mode !in setOf("summary", "user-model", "dui", "all")) {
        println("Usage: ./gradlew runMaintTest --args=\"summary|user-model|dui|all\"")
        return@runBlocking
    }

    val testOutputDir = Paths.get("/tmp/conduit-maint-test")

    // Start with a clean output directory for every run.
    withContext(Dispatchers.IO) {
        if (Files.exists(testOutputDir)) {
            Files.walk(testOutputDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
        Files.createDirectories(testOutputDir)
    }

    println("=== Conduit Maint Test ===")
    println("Mode:      $mode")
    println("Chats dir: ${AppUtils.getChatsDir()}")
    println("Output:    $testOutputDir")

    if (mode == "dui") {
        runDuiTest()
        return@runBlocking
    }

    // ---------------------------------------------------------------------
    // Create the native Conduit instance.
    // ---------------------------------------------------------------------

    val conduitPtr = LlmPortal.createConduit(2048)

    try {
        val scope = this

        // -----------------------------------------------------------------
        // Create AppState.
        // -----------------------------------------------------------------

        val state = AppState.createNew(
            conduitPtr = conduitPtr,
            scope = scope
        )

        // -----------------------------------------------------------------
        // Load chats exactly as the application does.
        // -----------------------------------------------------------------

        state.chatsList = ChatsList().also {
            it.build()
        }

        println("Chats loaded: ${state.chatsList.items.size}")

        // -----------------------------------------------------------------
        // Initialize the system expert.
        // -----------------------------------------------------------------

        val systemExpert = state.systemExpert

        val modelPath = Paths.get(
            AppUtils.getAppDir(),
            systemExpert.modelPath
                ?: error("System expert has no model path")
        ).toAbsolutePath().normalize()

        println("System model: $modelPath")

        systemExpert.sessionPtr = withContext(Dispatchers.IO) {
            LlmPortal.initialize(
                conduitPtr,
                modelPath.toString()
            )
        }

        try {
            // =============================================================
            // MAINTENANCE CYCLE
            // =============================================================

            if (mode == "summary" || mode == "all") {
                println()
                println("--- Chat Summary Maintenance ---")

                Maintenance.runChatSummaryMaintenance(state)
            }

            if (mode == "user-model" || mode == "all") {
                println()
                println("--- User Model Maintenance ---")

                Maintenance.runUserModelMaintenance(state)
            }

            // =============================================================
            // SNAPSHOT RESULTS
            // =============================================================

            val chatsDir = Paths.get(AppUtils.getChatsDir())
            val summariesDir = chatsDir.resolve("chat-summaries")
            val userModelFile =
                Paths.get(AppUtils.getAppDir(), ".conduit-user-model.json")

            println()
            println("--- Results ---")

            if (Files.exists(summariesDir)) {
                val summaries = withContext(Dispatchers.IO) {
                    Files.list(summariesDir).use { stream ->
                        stream
                            .filter { it.fileName.toString().endsWith(".json") }
                            .toList()
                    }
                }

                println("Chat summaries: ${summaries.size}")

                for (summary in summaries) {
                    println("  ${summary.fileName}")
                }
            } else {
                println("No chat summaries directory.")
            }

            if (Files.exists(userModelFile)) {
                val snapshot = testOutputDir.resolve(".conduit-user-model.json")

                withContext(Dispatchers.IO) {
                    Files.copy(
                        userModelFile,
                        snapshot
                    )
                }

                println("User model copied to: $snapshot")
                println()
                println("--- .conduit-user-model.json ---")
                println(userModelFile.readText())
                println()
                println("--- DUI Ranking ---")

                val duiRanking = UserModel.getDuiRankingForTest()
                for ((index, entry) in duiRanking.withIndex()) {
                    println(
                        "%3d  %.6f  %s".format(
                            index + 1,
                            entry.second,
                            entry.first
                        )
                    )
                }
            } else {
                println("No .conduit-user-model.json produced.")
            }

        } finally {
            LlmPortal.conduitLib.conduit_destroy_session(
                conduitPtr,
                systemExpert.sessionPtr
            )
            systemExpert.sessionPtr = null
        }

    } finally {
        LlmPortal.destroyConduit(conduitPtr)
    }

    println()
    println("=== Maint Test Complete ===")
}

private fun runDuiTest() {
    println()
    println("=== DUI v1 Synthetic Test ===")

    val now = 1_800_000_000_000L
    val day = 24L * 60L * 60L * 1000L

    val features = buildList {
        // Highly persistent and recent.
        add(
            ConduitUserModelFeature(
                text = "The user has a long-standing interest in mathematics.",
                firstSeen = now - 60 * day,
                lastSeen = now - 1 * day,
                observationCount = 20
            )
        )

        // Highly persistent but old.
        add(
            ConduitUserModelFeature(
                text = "The user has a long-standing interest in classical music.",
                firstSeen = now - 180 * day,
                lastSeen = now - 120 * day,
                observationCount = 20
            )
        )

        // Recent but seen only once.
        add(
            ConduitUserModelFeature(
                text = "The user recently mentioned a particular book.",
                firstSeen = now,
                lastSeen = now,
                observationCount = 1
            )
        )

        // Old and seen once.
        add(
            ConduitUserModelFeature(
                text = "The user once mentioned a temporary implementation detail.",
                firstSeen = now - 180 * day,
                lastSeen = now - 180 * day,
                observationCount = 1
            )
        )

        // Several intermediate cases.
        for (i in 1..96) {
            val count = (i % 10) + 1
            val ageDays = (i * 3L) % 150

            add(
                ConduitUserModelFeature(
                    text = "Synthetic user feature number $i with some additional descriptive text.",
                    firstSeen = now - ageDays * day,
                    lastSeen = now - ageDays * day,
                    observationCount = count
                )
            )
        }
    }

    println("Features: ${features.size}")
    println("Test time: $now")

    val testResult = UserModel.duiTest(features = features, now = now)
    val ranking = testResult.first
    val userModel = testResult.second

    println()
    println("--- DUI Ranking ---")
    println("rank  score      count  age(days)  feature")

    for ((rank, score, feature) in ranking) {
        val ageDays =
            (now - feature.lastSeen).coerceAtLeast(0L) /
                    (24.0 * 60.0 * 60.0 * 1000.0)

        println(
            "%4d  %9.6f  %5d  %9.2f  %s".format(
                rank,
                score,
                feature.observationCount,
                ageDays,
                feature.text
            )
        )
    }

    println()
    println("--- DUI Projection ---")
    println(userModel.text)

    println()
    println("Projected characters: ${userModel.text.length}")
}
*/
