package com.utilities.conduit.debug

import com.sun.tools.javac.tree.TreeInfo.args
import com.utilities.conduit.AppState
import com.utilities.conduit.Maintenance
import com.utilities.conduit.chat.ChatsList
import com.utilities.conduit.portals.LlmPortal
import com.utilities.conduit.utils.AppUtils
import com.utilities.conduit.utils.MaintenanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText

fun main(args: Array<String>) = runBlocking {
    val mode = args.firstOrNull()?.lowercase() ?: "all"

    if (mode !in setOf("summary", "user-model", "all")) {
        println("Usage: ./gradlew runMaintTest --args=\"summary|user-model|all\"")
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
                Paths.get(AppUtils.getAppDir(), "user-model.json")

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
                val snapshot = testOutputDir.resolve("user-model.json")

                withContext(Dispatchers.IO) {
                    Files.copy(
                        userModelFile,
                        snapshot
                    )
                }

                println("User model copied to: $snapshot")
                println()
                println("--- user-model.json ---")
                println(userModelFile.readText())
            } else {
                println("No user-model.json produced.")
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
